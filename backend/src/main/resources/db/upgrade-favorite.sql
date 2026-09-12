-- 「我的收藏」功能：新增 favorite 表
-- 用于已存在的数据库升级；全新建库请直接使用 schema.sql（已包含该表）。
-- 可重复执行（CREATE TABLE IF NOT EXISTS）。
-- 执行：mysql -u root -p --default-character-set=utf8mb4 < upgrade-favorite.sql

USE school_library;

-- 读者收藏书目：同一读者对同一本书只允许一条（唯一键 uk_favorite_reader_book）
CREATE TABLE IF NOT EXISTS favorite (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    reader_id  BIGINT   NOT NULL,
    book_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorite_reader_book (reader_id, book_id),
    KEY idx_favorite_reader_created (reader_id, created_at),
    CONSTRAINT fk_favorite_reader FOREIGN KEY (reader_id) REFERENCES reader (id),
    CONSTRAINT fk_favorite_book FOREIGN KEY (book_id) REFERENCES book (id)
) ENGINE = InnoDB;

-- 给示例读者 student1 预置两条收藏（已存在则跳过，不覆盖）
INSERT INTO favorite (reader_id, book_id, created_at)
SELECT r.id, b.id, NOW()
FROM reader r, book b
WHERE r.account = 'student1'
  AND b.isbn IN ('978-7-111-40701-0', '978-7-544-27478-4')
  AND NOT EXISTS (
      SELECT 1 FROM favorite f WHERE f.reader_id = r.id AND f.book_id = b.id
  );
