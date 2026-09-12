-- ============================================================================
-- 升级脚本：逾期提醒改为读者私有消息
--
-- 背景：读者 A 的图书逾期提醒只应 A 读者可见。提醒不再生成全局公告
--       （announcement 的 REMINDER 类型废弃），改为带 type = 'REMINDER'
--       标记的私有站内消息（notification，按 reader_id 隔离），
--       读者端弹窗与消息中心都以此区分。
--
-- 适用：已经建过库的环境（全新环境直接执行 db/schema.sql 即可，无需本脚本）。
-- 执行：mysql -uroot -p school_library < db/upgrade-notification-type.sql
-- 幂等：ALTER TABLE ADD COLUMN 不支持 IF NOT EXISTS，若报 "Duplicate column
--       name: 'type'" 说明已经执行过，忽略即可。
-- ============================================================================

ALTER TABLE notification
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'NORMAL'
    COMMENT '消息类型：NORMAL 普通 / REMINDER 逾期到期提醒' AFTER content;
