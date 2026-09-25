# 学校图书借阅系统

基于 **Vue 3 + Element Plus**（前端）与 **Spring Boot 3 + MyBatis-Plus + MySQL + Redis**（后端）的校园图书馆借阅管理系统，支持**公共前台 / 读者自助后台 / 管理后台**三端，桌面与移动端自适应。

> 当前视觉主题为「暖木书斋」（书棕 + 琥珀 + 暖米黄，衬线标题），规范见 [design-system/school-library/MASTER.md](design-system/school-library/MASTER.md)。

## 功能速览

| 端 | 主要功能 |
|---|---|
| 公共前台 | 馆藏检索与图书详情、本馆概况（简介/规章/楼层）、读者服务与活动、公告、自助注册 |
| 读者后台（学生/教师） | 自助借阅、我的借阅与续借、我的收藏、我的罚款与在线缴纳、站内通知与到期提醒、帮助与反馈（FAQ/在线咨询/留言） |
| 管理后台（管理员） | 图书编目与副本管理、Excel 批量导入导出、读者管理与停借、借阅流通（代借/代还/续借）、逾期罚款管理、公告与活动管理、FAQ/留言/实时咨询、数据概览 |

核心流通规则：学生可借 5 本、教师 10 本；借期 10 分钟（演示口径，`app.library.loan-minutes` 可调）；可续借 1 次；逾期罚款 0.10 元/分钟、单本封顶 144 元；有未缴罚单自动停借，缴清自动恢复。详见 [docs/business-flows.md](docs/business-flows.md)。

## 快速启动

### 方式一：Docker Compose（推荐）

```bash
cp .env.example .env        # 按需修改 DB_PASSWORD（默认 library123）
docker compose up -d --build
```

启动后访问：

- 前端站点：<http://localhost:5173>
- 后端 API：<http://localhost:8080>
- 接口文档（Knife4j）：<http://localhost:8080/doc.html>

### 方式二：本机开发

前置要求：JDK 17+、Maven 3.8+、Node 18+、MySQL 8、Redis 7。

```bash
# 1. 数据库：建库 school_library（建表与种子数据由后端启动时自动执行）
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS school_library DEFAULT CHARACTER SET utf8mb4"

# 2. 后端（默认连接 127.0.0.1:3306，密码取 DB_PASSWORD，默认 123456）
cd backend && mvn spring-boot:run

# 3. 前端
cd frontend && npm install && npm run dev
```

> Windows 下 5173 可能落在系统排除端口区间内，此时用 `npm run dev -- --port 5342 --strictPort` 换端口。更多开发环境注意事项见 [docs/troubleshooting.md](docs/troubleshooting.md)。

### 默认测试账号

| 账号 | 密码 | 角色 |
|---|---|---|
| admin1 | pass123 | 系统管理员 |
| student1 | pass123 | 学生读者（张同学） |
| teacher1 | pass123 | 教师读者（李老师） |

## 目录结构

```
├── backend/                 # Spring Boot 3 后端
│   └── src/main/resources/
│       ├── db/schema.sql    # 建表脚本（12 表，幂等）
│       └── db/upgrade-*.sql # 增量升级脚本
├── frontend/                # Vue 3 + Element Plus 前端（三端单页应用）
│   └── src/
│       ├── views/site/      # 公共前台页面
│       ├── views/reader/    # 读者后台页面
│       ├── views/admin/     # 管理后台页面
│       └── style.css        # 设计令牌层（--sl-* + Element Plus 桥接）
├── docs/                    # 工程文档（见下）
├── design-system/           # 视觉设计系统（ui-ux-pro-max 生成）
├── openspec/                # 规格驱动的变更过程文档
├── docs/                    # 工程文档（含 appendix 业务原始资料、deployment 部署细则、handover 交接快照）
└── docker-compose.yml       # mysql + redis + app + frontend 四容器
```

## 文档索引

| 文档 | 内容 |
|---|---|
| [docs/architecture.md](docs/architecture.md) | 系统架构图、模块依赖、数据流、部署拓扑、技术选型 |
| [docs/database.md](docs/database.md) | ER 图、12 张表结构与索引、数据字典（枚举/编码） |
| [docs/business-flows.md](docs/business-flows.md) | 借阅业务流程图、实体状态机、流通规则 |
| [docs/security.md](docs/security.md) | 认证会话、权限模型、密码策略、安全项现状 |
| [docs/development.md](docs/development.md) | 开发约定、环境与配置说明、代码结构规范 |
| [docs/troubleshooting.md](docs/troubleshooting.md) | 常见故障排查（实战踩坑记录） |
| [docs/deployment.md](docs/deployment.md) | 部署细则（本 README 快速启动的展开版） |
| [docs/DEPLOYMENT_CHECKLIST.md](docs/DEPLOYMENT_CHECKLIST.md) | 上线/部署检查清单 |
| [docs/handover.md](docs/handover.md) | 开发交接快照（功能清单/运行指南/测试账号） |
| [docs/rollback.md](docs/rollback.md) | 版本与数据库回滚方案 |
| [docs/user-guide.md](docs/user-guide.md) | 三端操作手册（含截图） |
| [CHANGELOG.md](CHANGELOG.md) | 迭代变更记录 |
| [docs/adr/](docs/adr/) | 架构决策记录（ADR） |
| [openspec/](openspec/) | 需求规格与变更过程（spec-driven） |

## 测试

```bash
cd frontend && npm test     # 前端单测（vitest，21 用例）
cd backend && mvn test      # 后端测试（H2 内存库，8 个测试类）
```

## 环境要求

- JDK 17+ / Maven 3.8+（后端）
- Node 18+ / npm 9+（前端）
- MySQL 8.0、Redis 7（会话存储）
- Docker Compose V2（容器化部署）
