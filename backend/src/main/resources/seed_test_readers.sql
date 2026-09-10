-- 新增 8 个测试读者（密码统一为 pass123，BCrypt $2a$12$）
-- 使用：mysql -h127.0.0.1 -u root -p school_library --default-character-set=utf8mb4 < seed_test_readers.sql
INSERT IGNORE INTO reader (account, password_hash, name, role, type, student_no, phone, status) VALUES
('test01', '$2a$12$BCukN6qGymLbTnt91obllu4mm9DwWu1w2WtqS742.FjQ6SRVWSpmO', '测试读者01', 'READER', 'STUDENT', 'TEST-0001', '13800000001', 'NORMAL'),
('test02', '$2a$12$BCukN6qGymLbTnt91obllu4mm9DwWu1w2WtqS742.FjQ6SRVWSpmO', '测试读者02', 'READER', 'STUDENT', 'TEST-0002', '13800000002', 'NORMAL'),
('test03', '$2a$12$BCukN6qGymLbTnt91obllu4mm9DwWu1w2WtqS742.FjQ6SRVWSpmO', '测试读者03', 'READER', 'STUDENT', 'TEST-0003', '13800000003', 'NORMAL'),
('test04', '$2a$12$BCukN6qGymLbTnt91obllu4mm9DwWu1w2WtqS742.FjQ6SRVWSpmO', '测试读者04', 'READER', 'STUDENT', 'TEST-0004', '13800000004', 'NORMAL'),
('test05', '$2a$12$BCukN6qGymLbTnt91obllu4mm9DwWu1w2WtqS742.FjQ6SRVWSpmO', '测试读者05', 'READER', 'STUDENT', 'TEST-0005', '13800000005', 'NORMAL'),
('test06', '$2a$12$BCukN6qGymLbTnt91obllu4mm9DwWu1w2WtqS742.FjQ6SRVWSpmO', '测试读者06', 'READER', 'STUDENT', 'TEST-0006', '13800000006', 'NORMAL'),
('test07', '$2a$12$BCukN6qGymLbTnt91obllu4mm9DwWu1w2WtqS742.FjQ6SRVWSpmO', '测试读者07', 'READER', 'TEACHER', 'TEST-0007', '13800000007', 'NORMAL'),
('test08', '$2a$12$BCukN6qGymLbTnt91obllu4mm9DwWu1w2WtqS742.FjQ6SRVWSpmO', '测试读者08', 'READER', 'TEACHER', 'TEST-0008', '13800000008', 'NORMAL');
