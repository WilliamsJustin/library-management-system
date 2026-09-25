-- ============================================================
-- 定向数据库回滚脚本（docs/rollback.md §2.2）
-- ⚠️ 使用前必读：
--   1. 先备份：mysqldump -uroot -p school_library > backup-<日期>.sql
--   2. 本脚本按"语句块"组织，只执行你确实需要回退的块，禁止整文件盲跑
--   3. 项目约定 upgrade-*.sql 只增不改不删，多数情况无需回滚（旧代码兼容新结构）
--   4. 标注 [不可逆数据风险] 的块执行前必须人工确认
-- ============================================================

USE school_library;

-- ------------------------------------------------------------
-- 块 1：回退 upgrade-favorite.sql（移除收藏功能）
-- [不可逆数据风险：删除表即丢失收藏数据]
-- ------------------------------------------------------------
-- DROP TABLE IF EXISTS favorite;

-- ------------------------------------------------------------
-- 块 2：回退 upgrade-help.sql（移除 FAQ / 留言 / 在线咨询）
-- [不可逆数据风险：FAQ 与留言、对话记录全部丢失；三表无外键，直接删表即可]
-- ------------------------------------------------------------
-- DROP TABLE IF EXISTS chat_message;
-- DROP TABLE IF EXISTS feedback_message;
-- DROP TABLE IF EXISTS faq;

-- ------------------------------------------------------------
-- 块 3：回退 upgrade-notification-type.sql（移除消息 type 列）
-- [不可逆数据风险：REMINDER/NORMAL 分类信息丢失，回滚后读者端弹窗逻辑不再区分]
-- ------------------------------------------------------------
-- ALTER TABLE notification DROP INDEX idx_notification_reader;
-- ALTER TABLE notification DROP COLUMN type;

-- ------------------------------------------------------------
-- 块 4：回退 upgrade-announcement-type.sql（移除公告 type 列）
-- ------------------------------------------------------------
-- ALTER TABLE announcement DROP COLUMN type;

-- ------------------------------------------------------------
-- 块 5：回退 upgrade-book-metadata.sql（移除书目扩展信息列）
-- [不可逆数据风险：出版日期/语言/定价/封面/简介数据丢失]
-- ------------------------------------------------------------
-- ALTER TABLE book
--   DROP COLUMN publish_date,
--   DROP COLUMN language,
--   DROP COLUMN price,
--   DROP COLUMN cover_url,
--   DROP COLUMN description;

-- ------------------------------------------------------------
-- 块 6：回退 upgrade-loan-minutes.sql（借期口径 分钟→天）
-- [不可逆数据风险：最高。回滚将把 DATETIME 借期口径改回天数，
--  存量 loan.due_date 值会失真；应用代码必须同时回退到天口径版本，
--  否则旧代码（分钟）+ 旧库（天）直接错乱。除非整版本回退，否则禁止执行]
-- ------------------------------------------------------------
-- ALTER TABLE loan MODIFY due_date DATE NULL;   -- 示意：口径回退语句，执行前必须核对旧版 schema 定义
