-- H2（MODE=MySQL）测试建表脚本。
-- 原 JPA 时代由 Hibernate 的 ddl-auto=create-drop 自动建表；换成 MyBatis-Plus 后不再有 ORM 自动建表，
-- 因此这里提供一份与 src/main/resources/db/schema.sql 等价、但语法可移植到 H2 的版本。
-- 与 MySQL 版的差异：去掉 ENGINE/CHARSET、把行内 KEY 改为独立 CREATE INDEX、TINYINT(1) 改为 BOOLEAN。

DROP TABLE IF EXISTS penalty;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS loan;
DROP TABLE IF EXISTS book_copy;
DROP TABLE IF EXISTS book;
DROP TABLE IF EXISTS reader;
DROP TABLE IF EXISTS announcement;
DROP TABLE IF EXISTS activity;

CREATE TABLE book (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    isbn      VARCHAR(20)  NOT NULL,
    title     VARCHAR(200) NOT NULL,
    author    VARCHAR(100) NULL,
    publisher VARCHAR(100) NULL,
    category  VARCHAR(50)  NULL,
    status    VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
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
    due_date      DATE        NOT NULL,
    returned_at   DATETIME    NULL,
    renewed_count INT         NOT NULL DEFAULT 0,
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

CREATE TABLE notification (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    reader_id  BIGINT       NOT NULL,
    content    VARCHAR(500) NOT NULL,
    created_at DATETIME     NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);
CREATE INDEX idx_notification_reader ON notification (reader_id, is_read);

CREATE TABLE announcement (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    title        VARCHAR(120)  NOT NULL,
    content      VARCHAR(1000) NOT NULL,
    pinned       BOOLEAN       NOT NULL DEFAULT FALSE,
    published_at DATETIME      NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_announcement_pinned_published ON announcement (pinned, published_at);

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
