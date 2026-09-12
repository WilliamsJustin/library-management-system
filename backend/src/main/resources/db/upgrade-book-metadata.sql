-- 为 book 表补充书目元数据字段（出版日期 / 语言 / 定价 / 封面 / 简介）
-- 用于已存在的数据库升级；全新建库请直接使用 schema.sql（已包含这些列）。
-- 注意：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，重复执行会报 1060 Duplicate column name，忽略即可。
-- 执行：mysql -u root -p --default-character-set=utf8mb4 < upgrade-book-metadata.sql

USE school_library;

ALTER TABLE book
    ADD COLUMN publish_date DATE          NULL COMMENT '出版日期',
    ADD COLUMN language     VARCHAR(20)   NULL COMMENT '语言',
    ADD COLUMN price        DECIMAL(10,2) NULL COMMENT '定价（元）',
    ADD COLUMN cover_url    VARCHAR(255)  NULL COMMENT '封面图片地址',
    ADD COLUMN description  VARCHAR(2000) NULL COMMENT '内容简介';

-- 回填示例图书的元数据（仅在字段为空时写入，避免覆盖管理员后续编辑）
UPDATE book SET publish_date = '2013-01-01', language = '中文', price = 128.00,
               description = '算法领域的经典教材，系统讲解排序、图论、动态规划、NP 完全性等核心内容，并给出严谨的复杂度分析，被全球众多高校选作算法课程用书。'
WHERE isbn = '978-7-111-40701-0' AND description IS NULL;

UPDATE book SET publish_date = '2014-05-01', language = '中文', price = 99.00,
               description = '从数据库基础、开发、优化到运维管理逐层展开，结合大量实例剖析 MySQL 的存储引擎、索引与查询优化，适合数据库开发与运维人员阅读。'
WHERE isbn = '978-7-115-42802-8' AND description IS NULL;

UPDATE book SET publish_date = '1996-12-01', language = '中文', price = 59.70,
               description = '中国古典四大名著之一，以贾、史、王、薛四大家族的兴衰为背景，展现封建社会的百态人生，被誉为中国古典小说的巅峰之作。'
WHERE isbn = '978-7-020-00000-1' AND description IS NULL;

UPDATE book SET publish_date = '2011-06-01', language = '中文', price = 39.50,
               description = '魔幻现实主义文学的代表作，讲述布恩迪亚家族七代人的传奇故事，折射拉丁美洲百年沧桑，作者因此获得诺贝尔文学奖。'
WHERE isbn = '978-7-544-27478-4' AND description IS NULL;

UPDATE book SET publish_date = '2014-07-01', language = '中文', price = 45.00,
               description = '高等学校工科数学基础教材，涵盖函数与极限、导数与微分、不定积分与定积分等核心内容，例题丰富、循序渐进。'
WHERE isbn = '978-7-115-41902-6' AND description IS NULL;

UPDATE book SET publish_date = '2007-03-01', language = '中文', price = 45.00,
               description = '国内高校广泛采用的数据结构教材，系统介绍线性表、栈、队列、树、图及其经典算法与存储实现。'
WHERE isbn = '978-7-111-40702-7' AND description IS NULL;
