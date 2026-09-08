# Design: 学校图书借阅系统

## Context

全新项目，无既有代码。技术栈已确认：Vue 3 前端 + Spring Boot 后端 + MySQL，前后端分离，部署于学校内网服务器或云上单实例。业务规则（借阅限额 5/10、借期 30/60 天、续借 1 次、逾期罚款与限制）见 specs/ 下各能力规格。

## Goals / Non-Goals

**Goals:**
- 前后端分离的单体后端应用，结构清晰，学校可长期维护
- 借出/归还/罚款等涉及多表的操作保证事务一致性
- 预约功能不在本期，但数据模型和续借校验预留扩展点

**Non-Goals:**
- 预约、统计报表、批量导入（第二期）
- 多校区/多租户
- 移动端 App（响应式 Web 即可）

## Decisions

### D1: 总体架构——前后端分离单体
Vue 3 SPA + Spring Boot REST API（单服务）+ MySQL。学校规模（数百至数万读者）下单体足够，避免微服务运维成本。
*备选*：Node.js 后端——被否，维护者 Java 背景更常见，且 Spring 声明式事务对借阅这种多表写入场景更稳。

### D2: 认证——Spring Security + JWT（无状态会话）
简单、水平扩展无障碍。角色：`ADMIN`、`READER`；读者携带身份类型（STUDENT/TEACHER）。前端路由守卫按角色分流到读者端/管理端界面。
*备选*：Session——单体下也可行，但 JWT 与 SPA 配合更直接。

### D3: 数据模型核心表
- `book`（书目）：id, isbn, title, author, publisher, category
- `book_copy`（副本）：id, book_id, barcode(唯一), location, status(IN_STOCK / BORROWED / WITHDRAWN)
- `reader`（读者）：id, account, password_hash, name, type(STUDENT/TEACHER), student_no, phone, status(NORMAL / RESTRICTED)
- `loan`（借阅记录）：id, copy_id, reader_id, borrowed_at, due_date, returned_at, renewed_count, status(ACTIVE/RETURNED/OVERDUE)
- `penalty`（罚款账单）：id, loan_id, reader_id, amount, status(UNPAID/PAID), paid_at

关键点：借阅规则（限额/借期）不落库硬编码于规则服务中，按 `reader.type` 读取常量表，便于将来调整；预约预留 `reservation` 表位置但本期不建。

### D4: 借阅规则引擎——独立服务层组件
`CirculationPolicy` 组件封装：上限（学生 5 / 教师 10）、借期（30/60 天）、续借次数（1）、续借前调用"预约检查接口"（本期实现返回恒为"无预约"的默认实现）。规格中的所有校验场景集中在这一层，便于测试。

### D5: 逾期处理——定时任务 + 归还时计算
- Spring `@Scheduled` 每日任务：扫描 ACTIVE 且 `due_date < now` 的借阅记录置为 OVERDUE、生成罚金、将读者置为受限；距 `due_date` 3 天内的发送提醒。
- 归还时若已逾期，同步生成罚金（与定时任务幂等，按 loan_id 唯一约束防重复）。
- 罚金标准（如每天 0.1 元）作为可配置常量。
*备选*：实时计算不落账单——被否，"缴纳后解除"需要明确的账单实体支撑对账。

### D6: 事务与并发控制
借出 = 校验 + 写 loan + 更新 copy.status，包裹在单个 `@Transactional` 方法中；copy 更新使用乐观锁（version 字段）防止同一副本被并发借出。归还+生成罚金同样单事务。

### D7: 前端结构
Vue 3 + Vite + Pinia + Vue Router + Element Plus。两个布局：读者端（检索、我的借阅、续借、修改密码）、管理端（编目、读者管理、借还操作台、罚款处理）。

### D8: API 风格
REST，`/api/` 前缀，按资源划分（`/books`、`/copies`、`/readers`、`/loans`、`/penalties`、`/auth`）。统一错误响应结构（code/message），前端据此展示校验与规则拒绝的提示。

## Risks / Trade-offs

- [定时任务依赖服务持续运行，宕机期间逾期状态不更新] → 归还时兜底计算逾期与罚金，保证最终一致
- [JWT 无刷新机制，读者长期使用中途过期] → 设置较长有效期（如 7 天）+ 过期统一跳转登录；后续可加 refresh token
- [并发借出同一副本] → 乐观锁 + 唯一性校验双保险
- [罚金标准/借期规则写死在代码常量] → 集中于单处配置，改规则不需改多处代码；如需运行时可调再升级为配置表

## Migration Plan

全新系统，无迁移。部署顺序：建库执行 schema → 启动后端 → 部署前端静态资源（或由后端/Nginx 托管）→ 创建初始管理员账号。回滚即停用新服务。

## Open Questions

- 逾期提醒的通知渠道（站内消息 / 邮件 / 短信）——不影响数据模型与任务拆分，实现任务时可按站内消息先行
