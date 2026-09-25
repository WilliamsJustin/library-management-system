# ADR-005: Docker Compose 单机四容器拓扑 + 环境变量覆盖配置

- 状态：已采纳（2026-09，系统初版期）

## 背景

部署目标是校内单机演示/课程交付：一台主机、无 K8s 平台、无多实例扩容需求；同时要保留"本机 IDEA 裸跑"的开发路径。

## 决策

`docker-compose.yml` 四容器：`mysql:8.0.36`（数据卷 mysql_data + healthcheck）、`redis:7-alpine`（仅会话，**关闭持久化**）、`app`（backend 构建 :8080，依赖前两者 healthy）、`frontend`（Nginx 托管 dist :5173→80）。环境差异不做 Spring profile 多文件，统一用**环境变量覆盖** `application.yml` 占位符（DB_PASSWORD、REDIS_HOST、SESSION_* 等）；`.env` 只放敏感项。

## 后果

- ✅ 一条命令起全套；健康检查串起启动顺序，避免"app 先于数据库起导致建表失败"
- ✅ 配置面收敛：无 dev/prod 配置文件漂移，环境差异一目了然（清单见 [docs/development.md §5](../development.md)）
- ✅ Redis 不持久化是有意的：会话丢了=重新登录，代价低于维护持久化
- ⚠️ 单点部署，无滚动更新/自愈能力（回滚方案见 [docs/rollback.md](../rollback.md)，镜像回退+快照恢复）
- ⚠️ `SPRING_SQL_INIT_MODE=always` 每次启动跑 schema.sql——依赖其幂等性，增量变更必须走 upgrade 脚本而非改历史脚本
