# 故障排查手册（SOP）

> 按症状索引。前 5 条均在本项目开发过程中实际踩坑并验证过解法，标 ✅。

## 1. ✅ 后端启动失败：`Port 8080 was already in use`

**根因排查链**（不要跳步，直接 kill 进程可能杀错）：

```bash
netstat -ano | findstr :8080          # 找到占用 PID
tasklist | findstr <PID>              # 看进程名
```

- 若是 **java**：可能是上次没退干净的 IDEA/后端实例，或本脚本遗留进程
- 若是 **com.docker.backend**：是 Docker 容器在占端口。`docker ps -a` 查容器（本项目曾出现 `school-library-app` 容器反复崩溃重启 RestartCount 1800+），处理：

```bash
docker update --restart=no school-library-app   # 先停自启，防止改完又起
docker stop school-library-app
docker rm school-library-app                    # 确认不再需要后删除
```

**预防**：IDEA 与 `docker compose up` 二选一跑后端，不要同时。

## 2. ✅ Windows 下 vite 起不来/换端口：端口落在排除区间

**症状**：`npm run dev` 报 5173/5174 权限错误（EACCES），换端口也一样。

**根因**：Windows Hyper-V/WSL 保留端口区间（本项目机器为 5141–5340 被排除）。查看：`netsh interface ipv4 show excludedportrange protocol=tcp`。

**解法**：用区间外端口启动：

```bash
npm run dev -- --port 5342 --strictPort --host 127.0.0.1
```

**预防**：把固定端口选在排除区间之外；不要用 `netsh int ipv4 add excludedportrange` 强占。

## 3. ✅ 内网/离线环境页面字体加载失败

**症状**：控制台 fonts.googleapis.com 请求超时，文字闪动。

**现状**：主题字体（Cormorant Garamond / Crimson Pro）已自托管在 `frontend/src/assets/fonts/*.woff2`，构建后无任何外网字体请求。

**验证**：DevTools Network 过滤 `fonts.gstatic|fonts.googleapis` 应为 0 条；`document.fonts.check('700 24px "Cormorant Garamond"')` 应为 true。

## 4. ✅ 登录后接口全部 401 / 前端反复跳登录页

**排查顺序**：

1. Redis 是否存活（会话全在 Redis）：`docker compose ps` 看 redis healthcheck；`redis-cli ping`
2. 会话命名空间是否被别的环境占用：`SESSION_NAMESPACE`（默认 `school:dev:session`）——多套环境共用一个 Redis 时改它隔离
3. 前端 localStorage 残留旧 token 而服务端会话已失效 → 启动后首个带数据的 API 请求返回 401，响应拦截器触发跳登录：清浏览器 localStorage 后刷新
4. 三处会话超时配置不一致（`SESSION_TIMEOUT_MINUTES` / `SESSION_TIMEOUT` / `SESSION_COOKIE_MAX_AGE`）导致 Cookie 先于会话过期

**深度验证**：会话超时机制有端到端验证脚本 [`scripts/verify-session.mjs`](../scripts/verify-session.mjs)（文件头注释含前置条件与用法，可压缩时间参数在 8081 起独立后端复现超时全链路）。

## 5. ✅ 表结构对不上 / 新增列没生效

**机制**：建表靠 `schema.sql`（`CREATE TABLE IF NOT EXISTS`——已存在的表**不会**被修改），增量变更靠 `db/upgrade-*.sql`。

**解法**：确认增量脚本是否执行过（脚本头部有说明）；本地可直接手工执行 `upgrade-<topic>.sql`。开发环境图省事可整库重建（有种子数据，见 §6）。

## 6. 想重置演示数据

```sql
DROP DATABASE school_library; CREATE DATABASE school_library DEFAULT CHARACTER SET utf8mb4;
```

重启后端即可：`SPRING_SQL_INIT_MODE=always` 重跑 schema.sql，`APP_SEED_ENABLED=true` 幂等重建种子账号（admin1/student1/teacher1，密码 pass123）。测试公告/读者种子脚本在 `backend/src/main/resources/seed_test_*.sql`。

## 7. Docker Compose 整套启动后前端 404 / API 502

- 顺序问题：frontend 依赖 app 启动，app 依赖 mysql/redis **健康检查通过**（首次拉镜像+建库可能要 1–2 分钟），稍候重试
- 看日志定位：`docker compose logs -f app`（数据源连不上通常是 `DB_PASSWORD` 与 mysql 容器初始化密码不一致——改 `.env` 后必须 `docker compose down -v` 重建数据卷）
- 端口冲突：5173/8080/3306 被占用按 §1 处理

## 8. 逾期罚款没生成 / 状态没变 OVERDUE

- 定时任务开关：`app.library.scheduling-enabled=true`（测试环境会关掉）
- 扫描周期 30 秒，等一轮再查；日志确认 `OverdueTask` 是否执行
- 到期提醒同理：只在到期前 5 分钟窗口内发一次（`due_reminder_sent` 已置位不再发）

## 9. Excel 导入失败

- 模板从各管理页「下载模板」获取（图书首列 ISBN、读者首列账号）；列顺序不可调
- 文件 ≤2MB、≤10000 行；不要用合并单元格；ISBN/账号已存在的行为按"更新/跳过"语义处理并在导入结果中提示
- 图片链接列填 http(s) 或站内 `/uploads/` 地址，留空即清除封面

## 10. 前端测试/构建报错

- `npm test`：21 个用例基于 vitest + happy-dom；组件测试失败先看是否改了 MobileCardList/useCrossPageSelection 的 props/事件契约
- `npx vue-tsc --noEmit`：类型错误通常是视图与 `utils/listDisplay.ts`、API 类型不一致
- HMR 卡住/页面渲染停滞：重启 dev server；浏览器端硬刷新（Ctrl+Shift+R）

## 11. 快速自检清单（拿不准先跑这个）

```bash
curl http://127.0.0.1:8080/api/ping            # 后端活
docker compose ps                               # 容器健康状态
cd frontend && npm test                         # 前端 21 用例
```
