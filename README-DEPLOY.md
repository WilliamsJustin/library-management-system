# 学校图书管理系统 · 部署与运行说明

技术栈：Vue 3 + Spring Boot 3.2 + MySQL 8 + Element Plus。

## 1. 环境要求

| 组件 | 版本 |
| --- | --- |
| JDK | 17 |
| Maven | 3.9+ |
| Node.js | 18+ |
| MySQL | 8.0 |

## 2. 一键启动（Docker Compose，推荐）

无需在本机安装 JDK/Maven/Node，准备好 Docker 即可一键拉起全栈（MySQL + 后端 + 前端）：

```bash
cp .env.example .env          # 可选：修改 DB_PASSWORD / JWT_SECRET
./start.sh                    # Linux / macOS / Git Bash
# 或 start.bat                # Windows
```

- 前端：http://localhost:5173
- 后端：http://localhost:8080/api
- MySQL：localhost:3306（库名 `school_library`）

说明：`mysql` 服务通过 `MYSQL_DATABASE` 自动建库；`app` 服务在启动时由 `spring.sql.init` 执行 `schema.sql` 建表（`ddl-auto=none`，避免与 `validate` 时序冲突），并在库为空时写入种子数据（已做幂等保护）。相关文件：`docker-compose.yml`、`backend/Dockerfile`、`frontend/Dockerfile`、`frontend/nginx.conf`。

停止：`docker compose down`；停止并清空数据：`docker compose down -v`。

> 前置：需先启动 Docker Desktop（守护进程），否则 `docker compose` 无法连接。

## 3. 初始化数据库（本地 / 非 Docker 部署）

若不使用 Docker，需手动建库建表（只需首次）：

```bash
mysql -u root -p < backend/src/main/resources/db/schema.sql
```

该脚本会创建 `school_library` 库及全部表（book / book_copy / reader / loan / penalty / notification）。
应用使用 `spring.jpa.hibernate.ddl-auto: none`（表结构由本脚本负责，Hibernate 只做对象映射、不自动建表/校验），因此**表结构必须与 schema.sql 保持一致**。

## 4. 后端配置与启动

`backend/src/main/resources/application.yml` 中的敏感项通过环境变量覆盖；连接本地 MySQL 时必须通过 `DB_PASSWORD` 提供数据库密码：

```bash
# 连接本机已运行的 MySQL（示例密码 123456，请按实际修改）
export DB_PASSWORD=123456
export JWT_SECRET=至少32位的随机密钥
mvn -f backend spring-boot:run        # 默认端口 8080
```

也可打包后运行：

```bash
mvn -f backend package
java -jar backend/target/library-0.1.0-SNAPSHOT.jar
```

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
| 学生 | student1 | pass123 | 可借 5 本 / 30 天 |
| 教师 | teacher1 | pass123 | 可借 10 本 / 60 天 |

## 7. 业务规则（集中配置于 `CirculationPolicy` + `application.yml`）

- 借阅上限：学生 5 本、教师 10 本（在借状态计入统计）。
- 借期：学生 30 天、教师 60 天。
- 续借：每本最多 1 次，续借后顺延一个标准借期。
- 逾期：由定时任务（`OverdueTask`，每日 01:00）扫描，将到期未还标记为逾期并生成罚款，同时限制读者借阅资格、下发站内提醒；归还或续借时也会兜底处理。
- 罚款：`app.library.fine-per-day`（默认 0.10 元/天）× 逾期天数；缴清后自动恢复借阅资格。
- 到期前提醒：提前 `app.library.reminder-days`（默认 3 天）下发一次提醒。

## 8. 主要功能模块

- **读者管理**：增删改查、停借/恢复、分页与搜索。
- **图书编目**：图书增删改查、上架/下架、副本管理、Excel 批量导入（xlsx，首行表头：ISBN/书名/作者/出版社/分类）。
- **借阅流通**：借出、归还、续借、借阅查询（管理员可按读者/状态筛选，读者可查看本人借阅）。
- **逾期罚款**：罚款查询、缴费（管理员代缴或读者自缴）。
- **站内消息**：逾期/到期提醒，读者可在首页查看并一键已读。

## 9. 主要接口

| 模块 | 方法 + 路径 | 说明 |
| --- | --- | --- |
| 认证 | POST /api/auth/login | 登录获取 token |
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
