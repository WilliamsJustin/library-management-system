-- ============================================================================
-- 升级脚本：帮助与反馈（FAQ / 留言板 / 在线咨询）
--
-- 背景：新增「帮助与反馈」模块——前台悬浮窗 FAQ 检索 + 转人工、读者后台
--       （常见问题 / 在线咨询 / 留言反馈）、管理员后台（FAQ 管理 / 实时对话 /
--       留言回复）。需要三张新表。
--
-- 适用：已经建过库的环境（全新环境直接执行 db/schema.sql 即可，无需本脚本）。
-- 执行：mysql -uroot -p school_library < db/upgrade-help.sql
-- 幂等：CREATE TABLE IF NOT EXISTS，可重复执行。
-- ============================================================================

-- FAQ 常见问题（帮助与反馈知识库：前台悬浮窗 + 读者后台检索，管理员维护）
CREATE TABLE IF NOT EXISTS faq (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    question    VARCHAR(200) NOT NULL,
    answer      VARCHAR(1000) NOT NULL,
    enabled     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

-- 留言反馈（读者/游客留言，管理员回复）
CREATE TABLE IF NOT EXISTS feedback_message (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    reader_id     BIGINT       NULL,
    reader_name   VARCHAR(50)  NOT NULL,
    content       VARCHAR(500) NOT NULL,
    reply_content VARCHAR(500) NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'UNREPLIED',
    created_at    DATETIME     NOT NULL,
    replied_at    DATETIME     NULL,
    replied_by    VARCHAR(50)  NULL,
    PRIMARY KEY (id),
    KEY idx_feedback_reader (reader_id, created_at),
    KEY idx_feedback_status (status, created_at)
) ENGINE = InnoDB;

-- 在线咨询对话消息（按读者会话归属，前端轮询拉取）
CREATE TABLE IF NOT EXISTS chat_message (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    reader_id   BIGINT       NOT NULL,
    sender_role VARCHAR(10)  NOT NULL,
    sender_name VARCHAR(50)  NOT NULL,
    content     VARCHAR(500) NOT NULL,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_chat_reader (reader_id, id)
) ENGINE = InnoDB;
