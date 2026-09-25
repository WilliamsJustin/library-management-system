# 系统架构

> 技术栈：Vue 3.5 + Element Plus 2.14 / Spring Boot 3.2.5 + MyBatis-Plus / MySQL 8 + Redis 7 / Docker Compose
> 规格与变更历史见 [openspec/](../openspec/)；关键选型的取舍见 [docs/adr/](adr/)。

## 1. 容器部署拓扑

```mermaid
flowchart LR
    U[浏览器] -->|80| FE[frontend 容器\nNginx + Vue 静态资源]
    FE -->|/api 反向代理| APP[app 容器\nSpring Boot 3 :8080]
    APP -->|3306| MY[(mysql 容器\nMySQL 8.0.36\nschool_library)]
    APP -->|6379| RD[(redis 容器\nRedis 7-alpine\n仅会话，不持久化)]
```

- 4 个服务定义于 [docker-compose.yml](../docker-compose.yml)；app 依赖 mysql/redis 的健康检查通过后才启动
- 数据卷仅 `mysql_data`（Redis 只存会话，关闭 RDB/AOF——会话丢了就重新登录，不值得落盘）
- 本机开发时四个组件都可独立跑（前端 vite :5173/5342，后端 :8080，MySQL/Redis 本机安装）

## 2. 应用分层（后端）

```mermaid
flowchart TD
    C[controller 层\n13 个 @RestController] --> S[service 层\n业务逻辑 + CirculationPolicy 规则引擎]
    S --> M[mapper 层\nMyBatis-Plus BaseMapper]
    M --> DB[(MySQL 12 表)]
    T[task/OverdueTask\n30s 轮询调度] --> S
    SEC[config/SecurityConfig\nSpring Session 会话认证] --> C
```

- 统一响应与异常：`{code, message}`，400/401/403/404/409 语义化错误码；未登录 `UNAUTHORIZED`、会话过期 `SESSION_EXPIRED`
- 接口文档由 Knife4j 运行时生成（`/doc.html`），维护成本为零

## 3. 前端三端模块结构（单 SPA）

```mermaid
flowchart TD
    R[router/index.ts\n路由守卫：requiresAuth + role] --> PUB[views/site\n公共前台 11 页]
    R --> READER[views/reader\n读者后台 8 页]
    R --> ADMIN[views/admin\n管理后台 8 页]
    PUB & READER & ADMIN --> SHARED[共享层]
    SHARED --> TK[style.css 设计令牌层\n--sl-* + EP --el-* 桥接]
    SHARED --> COMP[components\nMobileCardList / DateRangeCombo /\nFloatingHelp / GlobalReminderPopup / help 组件族]
    SHARED --> U[composables & utils\nuseBreakpoint / useCrossPageSelection /\nlistDisplay / dateUtils]
```

- 三端同一 SPA、按角色路由守卫拆分：`/`（访客）、`/reader`（role=reader）、`/admin`（role=admin）
- 桌面/移动双形态：`useBreakpoint`（768px 断点，matchMedia 单例），重表格页「桌面 el-table / 手机 MobileCardList」双渲染
- 换肤体系：业务代码只引用 `--sl-*` 令牌，Element Plus 组件通过变量桥接自动跟随（换主题 ≈ 改一个文件，见 [ADR-004](adr/ADR-004-frontend-design-tokens.md)）

## 4. 核心数据流：借阅生命周期

```mermaid
sequenceDiagram
    participant R as 读者后台
    participant A as app(Spring Boot)
    participant DB as MySQL
    participant T as OverdueTask(30s)
    R->>A: POST /api/loans/self（选副本借出）
    A->>A: 校验：停借？超上限？副本在库？
    A->>DB: loan(ACTIVE, due=now+借期) + copy→BORROWED（乐观锁）
    T->>DB: 到期前 5 分钟 → 站内 REMINDER（幂等）
    T->>DB: 过期 → loan→OVERDUE + 罚款(UNPAID) + 读者→RESTRICTED
    R->>A: POST /api/loans/{id}/renew（≤1 次，逾期不可续）
    R->>A: POST /api/loans/{id}/self-return
    A->>DB: loan→RETURNED + copy→IN_STOCK；逾期则结算罚款
    R->>A: POST /api/penalties/{id}/pay（缴清）
    A->>DB: penalty→PAID；无未缴罚单 → 读者恢复 NORMAL
```

完整规则与状态机见 [business-flows.md](business-flows.md)。

## 5. 技术选型一览

| 领域 | 选型 | 理由摘要 | ADR |
|---|---|---|---|
| 认证 | Spring Session + Redis（替代 JWT） | 需要服务端可吊销的会话与统一超时 | [ADR-001](adr/ADR-001-spring-session-over-jwt.md) |
| 馆藏模型 | 图书/副本两级（book_copy） | 流通实体是物理副本，条码可追溯 | [ADR-002](adr/ADR-002-copy-based-catalog.md) |
| 过程 | OpenSpec spec-driven | 需求→规格→任务的可审计变更链 | [ADR-003](adr/ADR-003-openspec-workflow.md) |
| 前端主题 | CSS 设计令牌 + EP 变量桥接 | 换肤收敛到单文件 | [ADR-004](adr/ADR-004-frontend-design-tokens.md) |
| 部署 | Docker Compose 单机四容器 | 课程级规模，无需编排平台 | [ADR-005](adr/ADR-005-docker-compose-topology.md) |
| ORM | MyBatis-Plus | SQL 可控 + 通用 CRUD 免写 | [ADR-006](adr/ADR-006-mybatis-plus.md) |
| 前端形态 | 三端同一 SPA | 共享组件/令牌/登录态，部署简单 | [ADR-007](adr/ADR-007-single-spa-three-surfaces.md) |

## 6. 监控与可观测性现状

如实记录：**当前没有监控告警体系**（无 Prometheus/Grafana/日志聚合）。现有手段：

- 健康探活：`GET /api/ping`（Docker 层面 mysql/redis 有 healthcheck，app 无）
- 结构化日志输出到 `logs/` 目录（本机运行时）
- 业务对账口径：管理后台首页的流通/罚款统计概览（`GET /api/stats/overview`）

若需接入监控，最小改造成本路径：暴露 Spring Boot Actuator health/metrics 端点 + Compose 外挂 Prometheus 容器。
