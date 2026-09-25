# 学校图书管理系统 · 部署与运行说明

技术栈：Vue 3 + Spring Boot 3.2 + MySQL 8 + Redis 7 + Element Plus。

## 1. 环境要求

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| JDK | 17 | 后端运行 |
| Maven | 3.9+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| MySQL | 8.0 | 业务数据 |
| Redis | 7.x | **登录会话存储（必需）** |

> Redis 是必需组件：认证已由「JWT 无状态」改为「Spring Session 服务端会话」，
> 会话数据（含认证信息与最后活跃时间）存在 Redis 里，键名形如 `school:dev:session:sessions:<会话ID>`。
> 本机快速启动：`docker run -d --name school-library-redis -p 6379:6379 redis:7-alpine`。

## 2. 一键启动（Docker Compose，推荐）

无需在本机安装 JDK/Maven/Node，准备好 Docker 即可一键拉起全栈（MySQL + Redis + 后端 + 前端）：

```bash
cp .env.example .env          # 可选：修改 DB_PASSWORD
./start.sh                    # Linux / macOS / Git Bash
# 或 start.bat                # Windows
```

- 前端：http://localhost:5173
- 后端：http://localhost:8080/api
- MySQL：localhost:3306（库名 `school_library`）
- Redis：localhost:6379（只存会话，已关闭持久化）

说明：`mysql` 服务通过 `MYSQL_DATABASE` 自动建库；`app` 服务在启动时由 `spring.sql.init` 执行 `schema.sql` 建表（表结构不交给框架自动生成），并在库为空时写入种子数据（已做幂等保护）。`app` 依赖 `mysql` 与 `redis` 的健康检查通过后才启动。相关文件：`docker-compose.yml`、`backend/Dockerfile`、`frontend/Dockerfile`、`frontend/nginx.conf`。

停止：`docker compose down`；停止并清空数据：`docker compose down -v`。

> 前置：需先启动 Docker Desktop（守护进程），否则 `docker compose` 无法连接。

## 3. 初始化数据库（本地 / 非 Docker 部署）

若不使用 Docker，需手动建库建表（只需首次）：

```bash
mysql -u root -p < backend/src/main/resources/db/schema.sql
```

该脚本会创建 `school_library` 库及全部表（全部 12 张表（book / book_copy / reader / loan / penalty / favorite / notification / announcement / activity / faq / feedback_message / chat_message））。
持久层是 **MyBatis-Plus**（已不再使用 Spring Data JPA / Hibernate），表结构完全由本脚本维护，
应用不会自动建表也不会校验，因此**表结构必须与 `schema.sql` 保持一致**。
表结构后续如有变更（如借期改为分钟导致的 `loan.due_date` 由 DATE 改 DATETIME），按 `db/upgrade-*.sql` 依次升级。

## 4. 后端配置与启动

`backend/src/main/resources/application.yml` 中的敏感项与外部依赖均可通过环境变量覆盖：

```bash
# MySQL（示例密码 123456，请按实际修改）
export DB_PASSWORD=123456
# Redis（本地默认 127.0.0.1:6379；有密码才需要设 REDIS_PASSWORD）
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
mvn -f backend spring-boot:run        # 默认端口 8080
```

也可打包后运行：

```bash
mvn -f backend package
java -jar backend/target/library-0.1.0-SNAPSHOT.jar
```

> **Redis 必须先起来**：登录时会往 Redis 写会话，Redis 不通则无法登录。
> 验证连通性：`redis-cli ping` 应返回 `PONG`；查看会话键：`redis-cli keys 'school:dev:session:*'`。

> 注意：本机若使用 Git Bash 且 `mvn` 启动报“找不到或无法加载主类 org.codehaus.plexus.classworlds.launcher.Launcher”，是 Maven 启动脚本路径转换损坏所致，可改用 `java -classpath <maven>/boot/plexus-classworlds-*.jar org.codehaus.plexus.classworlds.launcher.Launcher` 直接启动，或用 IDE 运行 `LibraryApplication`。

首次启动（库表为空时）会自动写入种子数据（见第 6 节），可通过 `app.seed.enabled=false` 关闭。

## 5. 前端

```bash
cd frontend
npm install
npm run dev      # 开发服务器 http://localhost:5173，已配置 /api 代理到 8080
npm run build    # 产物输出至 frontend/dist
```

> 说明：开发模式下前端通过 Vite 代理访问 `/api`；生产部署可将 `dist/` 作为静态资源由 Spring Boot 托管，或自行部署到 Nginx 等。

## 6. 默认账号

| 角色 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 管理员 | admin1 | pass123 | 拥有全部管理权限 |
| 学生 | student1 | pass123 | 可借 5 本 / 10 分钟 |
| 教师 | teacher1 | pass123 | 可借 10 本 / 10 分钟 |

## 7. 业务规则（集中配置于 `CirculationPolicy` + `application.yml`）

> 借期等单位已由「天」改为「分钟」：`loan.due_date` 是 **DATETIME**（原先为 DATE）。
> 已有库需执行升级脚本 `backend/src/main/resources/db/upgrade-loan-minutes.sql`。

- 借阅上限：学生 5 本、教师 10 本（在借状态计入统计）。
- 借期：学生与教师一致，均为 **10 分钟**（`app.library.loan-minutes`）。
- 续借：每本最多 1 次，续借后顺延一个标准借期（即再顺延 10 分钟）。
- 逾期：由定时任务（`OverdueTask`）**每 `app.library.check-interval-ms`（默认 30 秒）轮询一次**，
  将到期未还标记为逾期并生成罚款，同时限制读者借阅资格、下发站内提醒；归还或续借时也会兜底处理。
- 罚款：`app.library.fine-per-minute`（默认 0.10 元/**分钟**）× 逾期分钟数；缴清后自动恢复借阅资格。
- 到期前提醒：提前 `app.library.reminder-minutes`（默认 5 分钟）下发**一次**提醒；
  靠 `loan.due_reminder_sent` 保证同一笔借阅不重复提醒（续借后会重置，重新享有一次提醒）。
- 测试环境用 `app.library.scheduling-enabled=false` 关闭定时器（借期只有 10 分钟、
  轮询只有 30 秒，定时器会跑进测试事务干扰断言）。

## 8. 登录态与会话超时（Spring Security + Spring Session + Redis）

认证方式：**服务端会话**（不再签发 JWT）。

- 登录成功 → 服务端创建会话并把认证信息写入会话，会话数据存 Redis；返回的 `token` 字段即**会话 ID**。
- 会话传输支持两条通道，任一可用即可：
  - **Cookie**：`LIBRARY_SESSION`，HttpOnly + SameSite=Lax，Max-Age 与空闲超时一致；
  - **请求头**：`Authorization: Bearer <会话ID>`（前端把会话 ID 存在 localStorage 后照旧这么发）。
- **空闲超时：30 分钟**。判定有两层：
  1. 自定义拦截器 `SessionTimeoutInterceptor`：每个请求刷新「最后活跃时间」，距上次活跃 ≥ 30 分钟即作废会话并返回
     `401 {"code":"SESSION_EXPIRED"}`，前端据此跳登录页并提示「会话已超时」；
  2. Spring Session 在 Redis 上的过期时间（同一时长）作为兜底，会话键会自动消失。
- 因此「**关闭窗口后 30 分钟内不再打开，就会退出登录**」：关掉页面后没有请求，最后活跃时间停在那一刻，
  超过 30 分钟会话即失效；只要在 30 分钟内回来（刷新或重新打开），会话仍在、无需重新登录。
- 退出登录：前端调用 `POST /api/auth/logout` 作废服务端会话（该接口匿名可访问且幂等，
  会话已失效时也返回成功，保证前端总能清理本地登录态）。

相关配置（三处必须保持一致，代码统一读 `app.session.timeout-minutes`）：

| 配置项 | 默认值 | 作用 |
| --- | --- | --- |
| `app.session.timeout-minutes` | 30 | 业务判定 + 会话 `maxInactiveInterval` + Cookie Max-Age |
| `spring.session.timeout` | 30m | Spring Session 对会话的兜底过期时间 |
| `server.servlet.session.cookie.max-age` | 30m | 会话 Cookie 的存活时间 |

相关类：`config/SessionConfig`、`security/LoginSessionManager`、`security/SessionTimeoutInterceptor`、
`security/CompositeHttpSessionIdResolver`、`security/SessionTimeoutPolicy`。

> 注意：会话超时是**服务端**行为，与浏览器是否关闭无关——只要 Redis 里的会话过期或「最后活跃时间」超阈值，
> 客户端带着旧凭证来也会被拒。前端 `useIdleTimeout` 只是提前提示，不承担判定。

## 9. 主要功能模块

- **读者管理**：增删改查、停借/恢复、分页与搜索。
- **图书编目**：图书增删改查、上架/下架、副本管理、Excel 批量导入（xlsx，首行表头：ISBN/书名/作者/出版社/分类）。
- **借阅流通**：借出、归还、续借、借阅查询（管理员可按读者/类型/状态筛选，读者可查看本人借阅）。
- **逾期罚款**：罚款查询、缴费（管理员代缴或读者自缴）。
- **站内消息**：逾期/到期提醒，读者可在首页查看并一键已读。

## 10. 主要接口

| 模块 | 方法 + 路径 | 说明 |
| --- | --- | --- |
| 认证 | POST /api/auth/login | 登录，返回会话 ID（会话存 Redis） |
| 认证 | POST /api/auth/logout | 退出登录，作废服务端会话 |
| 认证 | POST /api/auth/change-password | 修改密码 |
| 读者 | GET /api/readers | 列表（支持 keyword/type/status） |
| 读者 | POST/PATCH/PUT/DELETE /api/readers/... | 增改、停借、删 |
| 图书 | GET/POST /api/books | 列表、新增 |
| 图书 | GET /api/books/{id}、/copies | 详情、副本 |
| 图书 | POST /api/books/import | Excel 导入 |
| 借阅 | POST /api/loans | 借出（管理员，按账号+条码） |
| 借阅 | POST /api/loans/{id}/return、/renew | 归还、续借 |
| 借阅 | GET /api/loans、/my | 查询、我的借阅 |
| 罚款 | GET /api/penalties、/my | 列表 |
| 罚款 | POST /api/penalties/{id}/pay | 缴费 |
| 消息 | GET /api/notifications/my、/unread-count | 列表、未读数 |
| 消息 | POST /api/notifications/my/read-all | 全部已读 |

所有响应错误统一结构为 `{ "code": "...", "message": "..." }`。
会话超时返回 `401 {"code": "SESSION_EXPIRED", "message": "会话已超时，请重新登录"}`，未登录返回 `401 {"code": "UNAUTHORIZED"}`。
