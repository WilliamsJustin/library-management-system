# ADR-006: 持久层选 MyBatis-Plus 而非 Spring Data JPA

- 状态：已采纳（2026-09，系统初版期）

## 背景

后端约 12 表、中等查询复杂度：多数 CRUD 可生成，但流通域有几个关键查询（逾期扫描按 status+due_date、报表聚合、跨表统计）需要精确控制 SQL 与索引配合。

## 决策

选 **MyBatis-Plus**：通用 Mapper 承接单表 CRUD（免写 XML），复杂查询手写 SQL/Wrapper，分页插件接 `PageBar` 前端契约。JPA/Hibernate 备选被否。

## 后果

- ✅ SQL 透明：逾期扫描、罚款结算、统计概览的每条 SQL 可直读可优化（配合 `idx_loan_status_due` 等复合索引）
- ✅ 乐观锁注解、逻辑删除、自动填充等工程能力开箱即用
- ⚠️ 比多一层"Repository 抽象"更贴库——换数据库的成本更高；本项目 MySQL 定位明确，接受
- ⚠️ 团队需懂 SQL（对单人维护者不是问题）
