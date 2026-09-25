# ADR-001: 用 Spring Session + Redis 替代 JWT

- 状态：已采纳（2026-09，系统初版期）
- 关联：`.env.example` 中遗留的废弃项 `JWT_SECRET`；[docs/security.md](../security.md)

## 背景

初版认证实现了 JWT 无状态方案。但本项目有两个现实约束：① 管理端需要**即时踢人/吊销**能力（读者违规、管理员换岗）；② 读者后台有 30 分钟空闲超时的产品要求，且前端需要可靠的"会话即将过期"预警。

## 决策

改为**服务端会话**：Spring Session + Redis 存储，Cookie（`LIBRARY_SESSION`，HttpOnly/SameSite=Lax）为主通道，保留 `Authorization: Bearer <会话ID>` 供非浏览器客户端。JWT 相关代码（JwtService/JwtAuthFilter）下线。

## 后果

- ✅ 会话可即时吊销（登出即删 Redis 键）；超时统一由服务端控制，三处配置（登录 maxInactive / Cookie Max-Age / 拦截器判定）共用一个参数
- ✅ 免去 JWT 刷新令牌、黑名单等复杂度
- ⚠️ 引入 Redis 强依赖（会话不可用=全员无法登录）；已接受——Redis 本就是部署拓扑一员
- ⚠️ 无状态扩容能力下降（会话粘 Redis）；课程级单机部署无所谓
