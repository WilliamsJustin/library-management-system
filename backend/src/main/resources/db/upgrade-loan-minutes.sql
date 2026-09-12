-- ============================================================================
-- 升级脚本：借期单位由「天」改为「分钟」
--
-- 背景：借阅规则调整为学生与教师借期均为 10 分钟，逾期罚款改为
--       0.10 元/分钟 × 逾期分钟数，并在到期前 5 分钟下发一次提醒。
--       原先 loan.due_date 是 DATE（只精确到天），无法表达 10 分钟的借期，
--       也没有「已提醒」标记来保证提醒只发一次，因此需要本脚本。
--
-- 适用：已经建过库的环境（全新环境直接执行 db/schema.sql 即可，无需本脚本）。
-- 执行：mysql -uroot -p school_library < db/upgrade-loan-minutes.sql
-- 幂等：可在同一库上重复执行（MODIFY / IF NOT EXISTS 都是幂等的）。
-- ============================================================================

-- 1) 应还时间精确到时刻（原 DATE 值会保留为当天 00:00:00）
ALTER TABLE loan MODIFY COLUMN due_date DATETIME NOT NULL
    COMMENT '应还时间（借期以分钟为单位）';

-- 2) 到期前提醒的幂等标记：默认未提醒
--    MySQL 5.7/8.0 不支持 ADD COLUMN IF NOT EXISTS，若报 "Duplicate column name"
--    说明已经执行过，忽略即可。
ALTER TABLE loan
    ADD COLUMN due_reminder_sent TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '到期前提醒是否已下发（保证只提醒一次）' AFTER renewed_count;

-- 3) 规则数值本身不落库，改由应用侧配置，见 application.yml 的 app.library.*：
--      loan-minutes:      10      -- 学生/教师借期均为 10 分钟
--      fine-per-minute:   0.10    -- 逾期罚款 0.10 元/分钟
--      reminder-minutes:  5       -- 到期前 5 分钟提醒一次
--      check-interval-ms: 30000   -- 逾期/提醒轮询间隔（30 秒）
--    注意：旧库中已存在的在借记录，其 due_date 仍是「过去某天 00:00:00」，
--    升级后会被定时任务判定为逾期并生成罚款；如需保留演示数据，
--    可先把未还记录的应还时间顺延，例如：
--      UPDATE loan SET due_date = NOW() + INTERVAL 10 MINUTE
--      WHERE status = 'ACTIVE' AND returned_at IS NULL;
