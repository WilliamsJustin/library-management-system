-- 学校图书借阅系统 MySQL schema
-- 使用方法（见部署说明 README-DEPLOY.md）：
--   mysql -u root -p < backend/src/main/resources/db/schema.sql

CREATE DATABASE IF NOT EXISTS school_library
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE school_library;

-- 书目
CREATE TABLE IF NOT EXISTS book (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    isbn      VARCHAR(20)  NOT NULL,
    title     VARCHAR(200) NOT NULL,
    author    VARCHAR(100) NULL,
    publisher VARCHAR(100) NULL,
    category  VARCHAR(50)  NULL,
    status    VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE 在架 / INACTIVE 下架
    PRIMARY KEY (id),
    UNIQUE KEY uk_book_isbn (isbn)
) ENGINE = InnoDB;

-- 副本（可借实体）
CREATE TABLE IF NOT EXISTS book_copy (
    id       BIGINT      NOT NULL AUTO_INCREMENT,
    book_id  BIGINT      NOT NULL,
    barcode  VARCHAR(50) NOT NULL,
    location VARCHAR(100) NULL,
    status   VARCHAR(20) NOT NULL DEFAULT 'IN_STOCK',  -- IN_STOCK / BORROWED / WITHDRAWN
    version  BIGINT      NOT NULL DEFAULT 0,           -- 乐观锁
    PRIMARY KEY (id),
    UNIQUE KEY uk_copy_barcode (barcode),
    KEY idx_copy_book (book_id),
    CONSTRAINT fk_copy_book FOREIGN KEY (book_id) REFERENCES book (id)
) ENGINE = InnoDB;

-- 读者（含管理员账号，role = ADMIN）
CREATE TABLE IF NOT EXISTS reader (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    account       VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    name          VARCHAR(50)  NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'READER', -- ADMIN / READER
    type          VARCHAR(20)  NOT NULL DEFAULT 'STUDENT', -- STUDENT / TEACHER
    student_no    VARCHAR(30)  NOT NULL,                  -- 学号或工号
    phone         VARCHAR(30)  NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'NORMAL', -- NORMAL / RESTRICTED
    PRIMARY KEY (id),
    UNIQUE KEY uk_reader_account (account),
    UNIQUE KEY uk_reader_student_no (student_no)
) ENGINE = InnoDB;

-- 借阅记录
CREATE TABLE IF NOT EXISTS loan (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    copy_id       BIGINT      NOT NULL,
    reader_id     BIGINT      NOT NULL,
    borrowed_at   DATETIME    NOT NULL,
    due_date      DATE        NOT NULL,
    returned_at   DATETIME    NULL,
    renewed_count INT         NOT NULL DEFAULT 0,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE / RETURNED / OVERDUE
    version       BIGINT      NOT NULL DEFAULT 0,        -- 乐观锁
    PRIMARY KEY (id),
    KEY idx_loan_copy (copy_id),
    KEY idx_loan_reader (reader_id),
    KEY idx_loan_status_due (status, due_date),
    CONSTRAINT fk_loan_copy FOREIGN KEY (copy_id) REFERENCES book_copy (id),
    CONSTRAINT fk_loan_reader FOREIGN KEY (reader_id) REFERENCES reader (id)
) ENGINE = InnoDB;

-- 罚款账单（loan_id 唯一 => 定时任务与归还兜底幂等）
CREATE TABLE IF NOT EXISTS penalty (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    loan_id    BIGINT        NOT NULL,
    reader_id  BIGINT        NOT NULL,
    amount     DECIMAL(10,2) NOT NULL,
    status     VARCHAR(20)   NOT NULL DEFAULT 'UNPAID', -- UNPAID / PAID
    created_at DATETIME      NOT NULL,
    paid_at    DATETIME      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_penalty_loan (loan_id),
    KEY idx_penalty_reader_status (reader_id, status),
    CONSTRAINT fk_penalty_loan FOREIGN KEY (loan_id) REFERENCES loan (id),
    CONSTRAINT fk_penalty_reader FOREIGN KEY (reader_id) REFERENCES reader (id)
) ENGINE = InnoDB;

-- 站内消息（逾期提醒）
CREATE TABLE IF NOT EXISTS notification (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    reader_id  BIGINT       NOT NULL,
    content    VARCHAR(500) NOT NULL,
    created_at DATETIME     NOT NULL,
    is_read     TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_notification_reader (reader_id, is_read)
) ENGINE = InnoDB;

-- 公共公告（首页展示，对所有访客可见）
CREATE TABLE IF NOT EXISTS announcement (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    title       VARCHAR(120) NOT NULL,
    content     VARCHAR(1000) NOT NULL,
    pinned      TINYINT(1)   NOT NULL DEFAULT 0,
    published_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_announcement_pinned_published (pinned, published_at)
) ENGINE = InnoDB;

-- 读者活动（前台"读者活动"栏目展示，对所有访客可见）
CREATE TABLE IF NOT EXISTS activity (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    title      VARCHAR(120) NOT NULL,
    content    VARCHAR(1000) NOT NULL,
    tag        VARCHAR(20)  NOT NULL,                 -- 类别标签：校级/培训/沙龙/活动/竞赛
    pinned     TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL,                 -- 发布时间，系统自动生成（精确到分钟）
    PRIMARY KEY (id),
    KEY idx_activity_pinned_created (pinned, created_at)
) ENGINE = InnoDB;
