-- H2（MODE=MySQL）测试建表脚本。
-- 原 JPA 时代由 Hibernate 的 ddl-auto=create-drop 自动建表；换成 MyBatis-Plus 后不再有 ORM 自动建表，
-- 因此这里提供一份与 src/main/resources/db/schema.sql 等价、但语法可移植到 H2 的版本。
-- 与 MySQL 版的差异：去掉 ENGINE/CHARSET、把行内 KEY 改为独立 CREATE INDEX、TINYINT(1) 改为 BOOLEAN。

DROP TABLE IF EXISTS favorite;
DROP TABLE IF EXISTS penalty;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS loan;
DROP TABLE IF EXISTS book_copy;
DROP TABLE IF EXISTS book;
DROP TABLE IF EXISTS reader;
DROP TABLE IF EXISTS announcement;
DROP TABLE IF EXISTS activity;
DROP TABLE IF EXISTS faq;
DROP TABLE IF EXISTS feedback_message;
DROP TABLE IF EXISTS chat_message;

CREATE TABLE book (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    isbn         VARCHAR(20)   NOT NULL,
    title        VARCHAR(200)  NOT NULL,
    author       VARCHAR(100)  NULL,
    publisher    VARCHAR(100)  NULL,
    category     VARCHAR(50)   NULL,
    publish_date DATE          NULL,
    language     VARCHAR(20)   NULL,
    price        DECIMAL(10,2) NULL,
    cover_url    VARCHAR(255)  NULL,
    description  VARCHAR(2000) NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    CONSTRAINT uk_book_isbn UNIQUE (isbn)
);

CREATE TABLE book_copy (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    book_id  BIGINT       NOT NULL,
    barcode  VARCHAR(50)  NOT NULL,
    location VARCHAR(100) NULL,
    status   VARCHAR(20)  NOT NULL DEFAULT 'IN_STOCK',
    version  BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_copy_barcode UNIQUE (barcode),
    CONSTRAINT fk_copy_book FOREIGN KEY (book_id) REFERENCES book (id)
);
CREATE INDEX idx_copy_book ON book_copy (book_id);

CREATE TABLE reader (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    account       VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    name          VARCHAR(50)  NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'READER',
    type          VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    student_no    VARCHAR(30)  NOT NULL,
    phone         VARCHAR(30)  NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    PRIMARY KEY (id),
    CONSTRAINT uk_reader_account UNIQUE (account),
    CONSTRAINT uk_reader_student_no UNIQUE (student_no)
);

CREATE TABLE loan (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    copy_id       BIGINT      NOT NULL,
    reader_id     BIGINT      NOT NULL,
    borrowed_at   DATETIME    NOT NULL,
    due_date      DATETIME    NOT NULL,
    returned_at   DATETIME    NULL,
    renewed_count INT         NOT NULL DEFAULT 0,
    due_reminder_sent BOOLEAN   NOT NULL DEFAULT FALSE,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version       BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_loan_copy FOREIGN KEY (copy_id) REFERENCES book_copy (id),
    CONSTRAINT fk_loan_reader FOREIGN KEY (reader_id) REFERENCES reader (id)
);
CREATE INDEX idx_loan_copy ON loan (copy_id);
CREATE INDEX idx_loan_reader ON loan (reader_id);
CREATE INDEX idx_loan_status_due ON loan (status, due_date);

CREATE TABLE penalty (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    loan_id    BIGINT        NOT NULL,
    reader_id  BIGINT        NOT NULL,
    amount     DECIMAL(10,2) NOT NULL,
    status     VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
    created_at DATETIME      NOT NULL,
    paid_at    DATETIME      NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_penalty_loan UNIQUE (loan_id),
    CONSTRAINT fk_penalty_loan FOREIGN KEY (loan_id) REFERENCES loan (id),
    CONSTRAINT fk_penalty_reader FOREIGN KEY (reader_id) REFERENCES reader (id)
);
CREATE INDEX idx_penalty_reader_status ON penalty (reader_id, status);

CREATE TABLE favorite (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    reader_id  BIGINT   NOT NULL,
    book_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_favorite_reader_book UNIQUE (reader_id, book_id)
);
CREATE INDEX idx_favorite_reader_created ON favorite (reader_id, created_at);

CREATE TABLE notification (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    reader_id  BIGINT       NOT NULL,
    content    VARCHAR(500) NOT NULL,
    type       VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    created_at DATETIME     NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);
CREATE INDEX idx_notification_reader ON notification (reader_id, is_read);

CREATE TABLE announcement (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    title        VARCHAR(120)  NOT NULL,
    content      VARCHAR(1000) NOT NULL,
    type         VARCHAR(20)   NOT NULL DEFAULT 'NORMAL',
    pinned       BOOLEAN       NOT NULL DEFAULT FALSE,
    published_at DATETIME      NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_announcement_pinned_published ON announcement (pinned, published_at);

CREATE TABLE faq (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    question   VARCHAR(200)  NOT NULL,
    answer     VARCHAR(1000) NOT NULL,
    enabled    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at DATETIME      NOT NULL,
    updated_at DATETIME      NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE feedback_message (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    reader_id     BIGINT       NULL,
    reader_name   VARCHAR(50)  NOT NULL,
    content       VARCHAR(500) NOT NULL,
    reply_content VARCHAR(500) NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'UNREPLIED',
    created_at    DATETIME     NOT NULL,
    replied_at    DATETIME     NULL,
    replied_by    VARCHAR(50)  NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_feedback_reader ON feedback_message (reader_id, created_at);

CREATE TABLE chat_message (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    reader_id   BIGINT       NOT NULL,
    sender_role VARCHAR(10)  NOT NULL,
    sender_name VARCHAR(50)  NOT NULL,
    content     VARCHAR(500) NOT NULL,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_chat_reader ON chat_message (reader_id, id);

CREATE TABLE activity (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    title      VARCHAR(120)  NOT NULL,
    content    VARCHAR(1000) NOT NULL,
    tag        VARCHAR(20)   NOT NULL,
    pinned     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at DATETIME      NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_activity_pinned_created ON activity (pinned, created_at);
