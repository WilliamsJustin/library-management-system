-- ============================================================================
-- 升级脚本：公告表新增 type 字段（逾期提醒全局弹窗）
--
-- 背景：触发逾期提醒时，定时任务会额外生成一条 type = 'REMINDER' 的全局公告；
--       前端挂载了全局弹窗组件，访问站内任一网页都会弹出 5 分钟内产生的提醒。
--       普通公告（管理员手工发布）type 保持 'NORMAL'，不受影响。
--
-- 适用：已经建过库的环境（全新环境直接执行 db/schema.sql 即可，无需本脚本）。
-- 执行：mysql -uroot -p school_library < db/upgrade-announcement-type.sql
-- 幂等：ALTER TABLE ADD COLUMN 不支持 IF NOT EXISTS，若报 "Duplicate column
--       name: 'type'" 说明已经执行过，忽略即可。
-- ============================================================================

ALTER TABLE announcement
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'NORMAL'
    COMMENT '公告类型：NORMAL 普通 / REMINDER 逾期提醒弹窗' AFTER content;
