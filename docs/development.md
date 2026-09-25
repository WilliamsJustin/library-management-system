# 开发规范与环境配置

> 单人/小团队课程级项目的"够用"规范——只写本项目实际遵守的约定，不做形式化的 GitFlow/CR 流程。

## 1. 仓库与分支

- 主干开发：`main` 分支保持可运行；功能改造开短生命周期的特性分支，完成后合回
- 变更管理走 **OpenSpec spec-driven 流程**（本项目最重要的"开发规范"）：探索/提案 → `openspec/changes/<name>/`（proposal/specs/design/tasks）→ 实施 → validate → 归档。详见 [ADR-003](adr/ADR-003-openspec-workflow.md) 与 `openspec/config.yaml`
- 提交信息建议 `<scope>: <动作>` 中文简述（如 `frontend: 修复跨页全选锚点`）；每次可独立验证的变更一个提交，避免"一堆混提交"

## 2. 验证基线（合入前必跑）

```bash
cd frontend && npm test && npx vue-tsc --noEmit   # 21 单测 + 类型检查
cd backend && mvn test                            # 后端测试（H2 内存库）
```

涉及视觉的变更加 375/768/1440 三档截图走查（768 断点切换不回归是硬约束，见 specs/visual-theme）。

## 3. 代码约定

### 后端（Spring Boot 3 + MyBatis-Plus）

- 分层：`controller → service → mapper`，业务参数集中 `service/CirculationPolicy`，不散落 Controller
- 枚举一律 Java enum（`entity/` 下 7 个），状态列存枚举名字符串；新增状态先加枚举再写库
- 并发扣减（副本借出）必须走乐观锁（`version` 列），禁止"先查再写"
- 错误响应统一 `{code, message}`；新增接口同步更新 Knife4j 注解（`/doc.html` 即文档）

### 前端（Vue 3.5 `<script setup>` + TS）

- **色彩/字体/圆角/阴影只引用 `--sl-*` / `--el-*` 变量**，禁止新增裸色值（换肤体系的命门，见 [ADR-004](adr/ADR-004-frontend-design-tokens.md)）
- 可复用的列表交互放 `components/` / `composables/`（MobileCardList、DateRangeCombo、useCrossPageSelection），新列表页先看现有封装再动手
- 移动端样式只允许出现在 `@media (max-width: 768px)` 内或 `isMobile` 分支中（桌面优先铁律）
- 术语用 `CONTEXT.md` 词汇表口径（借阅流通/副本/停借/列表显示口径等）
- 单测：composables 与通用组件必须带测试；视图级改动靠截图走查

## 4. 多环境说明

| 环境 | 组成 | 访问 | 说明 |
|---|---|---|---|
| 本机开发 | 本机 MySQL/Redis + `mvn spring-boot:run` + `npm run dev`（:5173 或 5342） | localhost | 日常开发；热重载 |
| Docker 演示 | `docker compose up -d` 四容器 | 前端 :5173 / API :8080 / 文档 :8080/doc.html | 演示与部署形态 |

> 没有独立的测试/预发/生产环境；多环境差异全部通过环境变量覆盖（见下节），这是有意的简化（[ADR-005](adr/ADR-005-docker-compose-topology.md)）。

## 5. 配置说明

### 5.1 环境变量清单（application.yml 占位符 → 环境变量）

| 环境变量 | 默认值 | 含义 |
|---|---|---|
| `DB_PASSWORD` | `123456` | MySQL root 密码（Docker 部署同 `MYSQL_ROOT_PASSWORD`，默认 library123） |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_DATABASE` | 127.0.0.1 / 6379 / 0 | 会话 Redis 连接 |
| `SESSION_TIMEOUT` / `SESSION_COOKIE_MAX_AGE` / `SESSION_TIMEOUT_MINUTES` | 30m / 30m / 30 | **三处必须一致**（会话/Redis 兜底/Cookie） |
| `SESSION_NAMESPACE` | `school:dev:session` | Redis 键前缀，多环境隔离用 |
| `SPRING_SQL_INIT_MODE` | （Docker 内 always） | 启动执行 schema.sql |
| `APP_SEED_ENABLED` | （Docker 内 true） | 幂等种子账号 |

业务参数（`app.library.*`，改完重启生效）：`loan-minutes`（借期）、`fine-per-minute`（罚款单价）、`max-fine-minutes`（封顶分钟）、`reminder-minutes`（提前提醒）、`check-interval-ms`（扫描周期）、`scheduling-enabled`（调度开关，测试置 false）。

### 5.2 敏感配置

- `.env` 不入库（`.env.example` 仅占位）；`DB_PASSWORD` 生产改强密码
- `JWT_SECRET` 为**废弃遗留项**（认证已改 Spring Session），新环境无需配置
- `/doc.html` 匿名可访问——生产按 [security.md §6](security.md) 处理

## 6. 目录与文档同步

- 新增文档放 `docs/`，入口链接补进根 [README.md](../README.md) 文档索引
- 实施完的变更及时在 [CHANGELOG.md](../CHANGELOG.md) 记一行；架构级取舍写 [docs/adr/](adr/)
- agent 协作约定见 [AGENTS.md](../AGENTS.md)（issue 放 `.scratch/<feature>/`、triage 标签、术语表使用）
