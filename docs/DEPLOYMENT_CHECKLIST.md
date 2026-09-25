# 部署检查清单（Deployment Checklist）

> 每次部署/交付前逐项勾选。基础部署步骤见 [docs/deployment.md](deployment.md)（环境要求、Compose 一键启动、本机部署、账号、业务规则）；回滚预案见 [rollback.md](rollback.md)。

## A. 构建产物

- [ ] `cd frontend && npm install && npm run build` 成功，`dist/` 生成
- [ ] `cd frontend && npx vue-tsc --noEmit` 无类型错误
- [ ] `cd frontend && npm test` 21 个单测全绿
- [ ] `cd backend && mvn clean package -DskipTests=false` 打包成功，后端测试全绿
- [ ] Docker 镜像构建成功：`docker compose build`（frontend / app 两个镜像）

## B. 配置与环境

- [ ] `.env` 就位（参考 `.env.example`）：`DB_PASSWORD` 已改为部署环境强密码
- [ ] `.env` 中 **`JWT_SECRET` 已删除**（已废弃项，认证为 Spring Session）
- [ ] `SESSION_NAMESPACE` 与其他环境隔离（如 `school:prod:session`），避免共用 Redis 串会话
- [ ] 三处会话超时一致：`SESSION_TIMEOUT_MINUTES` = `SESSION_TIMEOUT` = `SESSION_COOKIE_MAX_AGE`（默认均 30m）
- [ ] 数据库连接串指向目标库；确认执行策略 `SPRING_SQL_INIT_MODE=always` + `db/upgrade-*.sql` 是否需要手工执行
- [ ] 生产建议：关闭 Knife4j 对外暴露（`knife4j.production=true` 或网络层封禁 `/doc.html`）

## C. 基础设施

- [ ] MySQL 8 可达，`school_library` 库存在（utf8mb4）；数据卷 `mysql_data` 策略已确认
- [ ] Redis 7 可达（`redis-cli ping` 通）；确认会话可接受不持久化（重启丢会话=全员重新登录）
- [ ] 容器端口无冲突：5173（frontend）、8080（app）、3306、6379——Windows 参见 [troubleshooting.md §1/§2](troubleshooting.md)
- [ ] `docker compose ps` 四服务状态：mysql/redis healthy → app Up → frontend Up

## D. 启动后冒烟（5 分钟）

- [ ] `curl http://127.0.0.1:8080/api/ping` 返回正常
- [ ] 前端站点可打开，未登录可见公共前台（首页/检索/公告/活动）
- [ ] admin1 / pass123 登录 → 管理后台首页统计有数
- [ ] student1 / pass123 登录 → 读者后台可检索并完成一次借阅 + 归还
- [ ] 借一本后等待/手调 `due_date`，确认 30s 轮询产生逾期与罚款、读者被停借
- [ ] 缴清罚款 → 读者自动恢复 NORMAL
- [ ] Knife4j `/doc.html` 可打开（或已按计划封禁）

## E. 数据与回滚预案

- [ ] 部署前数据库已备份（`mysqldump school_library > backup-$(date +%F).sql`）
- [ ] 本次若含 `upgrade-*.sql`：已核对对应回滚口径（[rollback.md](rollback.md) / [docs/sql/rollback.sql](sql/rollback.sql)）
- [ ] 上一版本镜像 tag/构建记录可追溯（`docker images`），可一键回退

## F. 交付物核对

- [ ] [README.md](../README.md) 快速启动步骤与当前版本一致
- [ ] [CHANGELOG.md](../CHANGELOG.md) 已补本次变更条目
- [ ] 测试账号清单已同步（当前：admin1 / student1 / teacher1，密码均 pass123）
