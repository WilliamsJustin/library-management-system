# 安全设计

> 本文如实记录系统**已实施**的安全机制与**未实施**项。技术基线：Spring Security 6 + Spring Session（Redis）+ BCrypt。

## 1. 认证机制（已实施）

- **服务端会话**：Spring Session + Redis，认证状态存服务端，浏览器只持会话 Cookie——与早期 JWT 方案的取舍见 [ADR-001](adr/ADR-001-spring-session-over-jwt.md)
- **双通道会话解析**：浏览器走 `LIBRARY_SESSION` Cookie（HttpOnly，SameSite=Lax，path=/）；非浏览器客户端可用 `Authorization: Bearer <会话ID>`
- **会话超时**：30 分钟空闲失效（登录时 `maxInactiveInterval`、Cookie Max-Age、拦截器空闲判定三处共用 `app.session.timeout-minutes`，另有 `spring.session.timeout` 作 Redis 兜底）；前端 `useIdleTimeout` 空闲预警，`401 SESSION_EXPIRED` 统一跳登录页
- **密码存储**：BCrypt 散列（`BCryptPasswordEncoder`），库中无明文
- **登录防提权**：登录响应按后端返回的角色路由（ADMIN→/admin，READER→/reader），前端守卫与后端方法级注解双重校验

## 2. 权限模型（已实施）

角色只有两个：`ROLE_ADMIN`、`ROLE_READER`（读者表内 `role` 列区分，读者类型 STUDENT/TEACHER 只影响借阅额度，不影响权限）。

| 资源 | 匿名 | READER | ADMIN |
|---|---|---|---|
| 书目/公告/活动 GET、FAQ 检索、留言提交、注册 | ✅ | ✅ | ✅ |
| 自助借阅/归还/续借、收藏、我的罚款缴纳、站内通知、留言与在线咨询 | ❌ | ✅ | ❌（提示换读者账号） |
| 编目/读者管理/代借代还/罚款代缴/公告活动管理/FAQ 与留言管理/实时客服/统计 | ❌ | ❌ | ✅ |

- 实现方式：`SecurityConfig` 放行白名单 + `@EnableMethodSecurity` 方法级 `hasRole(...)` 注解
- 越权行为统一返回：未登录 `401 UNAUTHORIZED`、无权限 `403 FORBIDDEN`（例：读者调管理员代缴接口）
- **数据越权防护**：我的罚款/我的通知/我的留言等"我的"接口一律从会话取 reader_id，不接受前端传入

## 3. 放行路径清单（SecurityConfig 白名单）

```
OPTIONS 全部；POST /api/auth/login | /api/auth/register | /api/auth/logout
GET  /api/ping/**；/error
匿名 GET：/api/books、/api/books/**、/api/announcements/**、/api/activities/**、/api/help/faq、/uploads/**
文档：/doc.html、/swagger-ui/**、/v3/api-docs/** 等
其余 anyRequest().authenticated()
```

> 注意：`/uploads/**`（用户上传的封面）匿名可读，属预期；不要把敏感文件放进该目录。

## 4. 注销与会话吊销（已实施）

`POST /api/auth/logout` 即时销毁 Redis 会话——服务端可吊销是选会话方案的核心收益。前端登出同时清理本地 token/user 缓存。

## 5. 未实施项与建议（如实记录）

| 项 | 现状 | 建议 |
|---|---|---|
| CSRF 防护 | `csrf.disable()`（纯 Cookie 会话下理论可 CSRF） | 若上生产：启用 Cookie_CSRF（SameSite=Lax 已缓解大部分），或保持 Bearer 双通道模式 |
| XSS | Vue 模板默认转义；富文本仅公告/活动内容（管理员输入） | 保持"不使用 v-html 渲染用户输入"；如需富文本引入 DOMPurify |
| 速率限制 | 无（登录接口可被暴力尝试） | 加 Bucket4j/网关层限流，登录失败锁定 |
| 数据脱敏 | 读者手机号管理端明文展示 | 如需合规：管理端列表对手机号中间四位打码 |
| HTTPS | 未配置（本地/内网 HTTP） | 生产部署在 Nginx 层终结 TLS |
| 密码策略 | 注册/导入默认密码 `pass123`；可改密 | 上线前强制首次登录改密；增加复杂度校验 |
| 审计日志 | 无操作审计表 | 如需追溯：记录管理员关键操作（代借/代缴/删除） |

## 6. 敏感配置清单

| 配置 | 位置 | 处理 |
|---|---|---|
| `DB_PASSWORD` | `.env`（不入库，`.env.example` 只有占位） | 生产改强密码；容器与本机连接共用 |
| `JWT_SECRET` | `.env.example` | **已废弃**（认证已改 Spring Session，JwtService 下线），可从 .env 删除 |
| `SESSION_NAMESPACE` | `application.yml`（默认 `school:dev:session`） | 环境隔离用，避免多套环境共享 Redis 会话 |
| Knife4j `/doc.html` | 匿名可访问 | 生产建议关闭（`knife4j.production=true`）或加 IP 白名单 |
