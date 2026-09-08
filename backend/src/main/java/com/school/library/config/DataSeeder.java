package com.school.library.config;

import com.school.library.entity.Announcement;
import com.school.library.entity.Book;
import com.school.library.entity.BookCopy;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderType;
import com.school.library.entity.UserRole;
import com.school.library.repository.AnnouncementRepository;
import com.school.library.repository.BookCopyRepository;
import com.school.library.repository.BookRepository;
import com.school.library.repository.ReaderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 种子数据初始化：首次启动（库表为空）时写入测试账号与示例图书。
 * 测试环境通过 app.seed.enabled=false 关闭。
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    public static final String DEFAULT_PASSWORD = "pass123";

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository copyRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedReaders();
        seedBooks();
        seedAnnouncements();
    }

    private void seedReaders() {
        if (readerRepository.count() > 0) {
            return;
        }
        readerRepository.save(reader("admin1", "系统管理员", UserRole.ADMIN, ReaderType.TEACHER, "STAFF-0001"));
        readerRepository.save(reader("student1", "张同学", UserRole.READER, ReaderType.STUDENT, "20240001"));
        readerRepository.save(reader("teacher1", "李老师", UserRole.READER, ReaderType.TEACHER, "T2001001"));
    }

    private void seedBooks() {
        if (bookRepository.count() > 0) {
            return;
        }
        seedBook("978-7-111-40701-0", "算法导论", "Thomas H. Cormen", "机械工业出版社", "计算机", 3);
        seedBook("978-7-115-42802-8", "深入浅出MySQL", "姜承尧", "人民邮电出版社", "计算机", 2);
        seedBook("978-7-020-00000-1", "红楼梦", "曹雪芹", "人民文学出版社", "文学", 2);
        seedBook("978-7-544-27478-4", "百年孤独", "加西亚·马尔克斯", "南海出版公司", "文学", 1);
        seedBook("978-7-115-41902-6", "高等数学", "同济大学数学系", "人民邮电出版社", "数学", 3);
        seedBook("978-7-111-40702-7", "数据结构", "严蔚敏", "机械工业出版社", "计算机", 2);
    }

    private void seedAnnouncements() {
        if (announcementRepository.count() > 0) {
            return;
        }
        announcementRepository.save(new Announcement(
                "关于图书馆秋季开馆时间的通知",
                "自9月1日起，图书馆开馆时间调整为 8:00–22:00，周末不休。请读者合理安排借阅时间。", true));
        announcementRepository.save(new Announcement(
                "新生入馆须知",
                "欢迎新同学！凭学号可在首页自助注册读者账号，注册后即可在线检索书目、预约与续借。", true));
        announcementRepository.save(new Announcement(
                "图书逾期提醒服务上线",
                "系统现已支持到期前3天自动推送提醒，避免产生滞纳金。请保持账号联系方式准确。", false));
        announcementRepository.save(new Announcement(
                "读书月系列活动预告",
                "本月将举办“经典共读”“文献检索培训”等系列活动，详情请关注“读者活动”栏目。", false));
    }

    private void seedBook(String isbn, String title, String author, String publisher, String category, int copyCount) {
        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        book.setPublisher(publisher);
        book.setCategory(category);
        book = bookRepository.save(book);

        for (int i = 1; i <= copyCount; i++) {
            BookCopy copy = new BookCopy();
            copy.setBook(book);
            copy.setBarcode("C" + book.getId() + String.format("-%03d", i));
            copy.setLocation("A区" + ((book.getId() % 5) + 1) + "排");
            copyRepository.save(copy);
        }
    }

    private Reader reader(String account, String name, UserRole role, ReaderType type, String studentNo) {
        Reader reader = new Reader();
        reader.setAccount(account);
        reader.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        reader.setName(name);
        reader.setRole(role);
        reader.setType(type);
        reader.setStudentNo(studentNo);
        return reader;
    }
}
