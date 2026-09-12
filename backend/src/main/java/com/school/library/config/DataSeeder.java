package com.school.library.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.library.entity.Activity;
import com.school.library.entity.Announcement;
import com.school.library.entity.Book;
import com.school.library.entity.BookCopy;
import com.school.library.entity.Faq;
import com.school.library.entity.Favorite;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderType;
import com.school.library.entity.UserRole;
import com.school.library.mapper.ActivityMapper;
import com.school.library.mapper.AnnouncementMapper;
import com.school.library.mapper.BookCopyMapper;
import com.school.library.mapper.BookMapper;
import com.school.library.mapper.FaqMapper;
import com.school.library.mapper.FavoriteMapper;
import com.school.library.mapper.ReaderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 种子数据初始化：首次启动（库表为空）时写入测试账号与示例图书。
 * 测试环境通过 app.seed.enabled=false 关闭。
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    public static final String DEFAULT_PASSWORD = "pass123";

    @Autowired
    private ReaderMapper readerMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BookCopyMapper copyMapper;

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private FaqMapper faqMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedReaders();
        seedBooks();
        seedExtraBooks();
        seedAnnouncements();
        seedActivities();
        seedFavorites();
        seedFaqs();
    }

    /** 帮助与反馈的 FAQ 知识库：表为空时写入常见问题（管理员可在后台增删改） */
    private void seedFaqs() {
        if (faqMapper.selectCount(null) > 0) {
            return;
        }
        faq("开馆时间是什么时候？", "图书馆开馆时间为每天 8:00–22:00，周末不休，法定节假日另行公告。");
        faq("最多能借几本书？借期多长？", "学生最多可同时借阅 5 本，教师最多 10 本；借期与罚款规则详见「读者服务」栏目或咨询管理员。");
        faq("如何续借图书？", "在借阅期内可进入读者后台「在线续借」为在借图书续借 1 次，续借后借期自动顺延一个标准借期；已逾期图书无法续借。");
        faq("逾期了会怎样？罚款怎么算？", "超过应还时间未归还即视为逾期，将按逾期时长产生罚款（0.10 元/分钟），且在缴清罚款前会被限制借阅。缴清后借阅资格自动恢复。");
        faq("图书到期会提醒吗？", "会。系统会在图书到期前自动推送提醒消息，请留意读者后台的消息通知，也可在「我的借阅」中查看应还时间。");
        faq("忘记密码怎么办？", "登录后可在页面右上角进入「修改密码」自助修改；如忘记原密码无法登录，请携带有效证件到图书馆服务台重置。");
        faq("找不到想借的书怎么办？", "可先在「图书检索」中更换关键词或按书名/作者/ISBN 精确检索；确认馆藏没有的图书，可在留言板留言荐购，管理员会定期查看。");
    }

    private void faq(String question, String answer) {
        Faq f = new Faq();
        f.setQuestion(question);
        f.setAnswer(answer);
        faqMapper.insert(f);
    }

    /** 给示例读者预置几条收藏，便于首次进入「我的收藏」就能看到内容 */
    private void seedFavorites() {
        if (favoriteMapper.selectCount(null) > 0) {
            return;
        }
        Reader student = readerMapper.selectOne(Wrappers.<Reader>lambdaQuery().eq(Reader::getAccount, "student1"));
        if (student == null) {
            return;
        }
        favoriteByIsbn(student.getId(), "978-7-111-40701-0");
        favoriteByIsbn(student.getId(), "978-7-544-27478-4");
    }

    private void favoriteByIsbn(Long readerId, String isbn) {
        Book book = bookMapper.selectOne(Wrappers.<Book>lambdaQuery().eq(Book::getIsbn, isbn));
        if (book != null) {
            favoriteMapper.insert(new Favorite(readerId, book.getId()));
        }
    }

    private void seedReaders() {
        if (readerMapper.selectCount(null) > 0) {
            return;
        }
        readerMapper.insert(reader("admin1", "系统管理员", UserRole.ADMIN, ReaderType.TEACHER, "STAFF-0001"));
        readerMapper.insert(reader("student1", "张同学", UserRole.READER, ReaderType.STUDENT, "20240001"));
        readerMapper.insert(reader("teacher1", "李老师", UserRole.READER, ReaderType.TEACHER, "T2001001"));
    }

    private void seedBooks() {
        if (bookMapper.selectCount(null) > 0) {
            return;
        }
        seedBook("978-7-111-40701-0", "算法导论", "Thomas H. Cormen", "机械工业出版社", "计算机",
                LocalDate.of(2013, 1, 1), "中文", new BigDecimal("128.00"),
                "算法领域的经典教材，系统讲解排序、图论、动态规划、NP 完全性等核心内容，并给出严谨的复杂度分析，"
                        + "被全球众多高校选作算法课程用书。", 3);
        seedBook("978-7-115-42802-8", "深入浅出MySQL", "姜承尧", "人民邮电出版社", "计算机",
                LocalDate.of(2014, 5, 1), "中文", new BigDecimal("99.00"),
                "从数据库基础、开发、优化到运维管理逐层展开，结合大量实例剖析 MySQL 的存储引擎、索引与查询优化，"
                        + "适合数据库开发与运维人员阅读。", 2);
        seedBook("978-7-020-00000-1", "红楼梦", "曹雪芹", "人民文学出版社", "文学",
                LocalDate.of(1996, 12, 1), "中文", new BigDecimal("59.70"),
                "中国古典四大名著之一，以贾、史、王、薛四大家族的兴衰为背景，展现封建社会的百态人生，"
                        + "被誉为中国古典小说的巅峰之作。", 2);
        seedBook("978-7-544-27478-4", "百年孤独", "加西亚·马尔克斯", "南海出版公司", "文学",
                LocalDate.of(2011, 6, 1), "中文", new BigDecimal("39.50"),
                "魔幻现实主义文学的代表作，讲述布恩迪亚家族七代人的传奇故事，折射拉丁美洲百年沧桑，"
                        + "作者因此获得诺贝尔文学奖。", 1);
        seedBook("978-7-115-41902-6", "高等数学", "同济大学数学系", "人民邮电出版社", "数学",
                LocalDate.of(2014, 7, 1), "中文", new BigDecimal("45.00"),
                "高等学校工科数学基础教材，涵盖函数与极限、导数与微分、不定积分与定积分等核心内容，"
                        + "例题丰富、循序渐进。", 3);
        seedBook("978-7-111-40702-7", "数据结构", "严蔚敏", "机械工业出版社", "计算机",
                LocalDate.of(2007, 3, 1), "中文", new BigDecimal("45.00"),
                "国内高校广泛采用的数据结构教材，系统介绍线性表、栈、队列、树、图及其经典算法与存储实现。", 2);
    }

    private void seedAnnouncements() {
        if (announcementMapper.selectCount(null) > 0) {
            return;
        }
        announcementMapper.insert(new Announcement(
                "关于图书馆秋季开馆时间的通知",
                "自9月1日起，图书馆开馆时间调整为 8:00–22:00，周末不休。请读者合理安排借阅时间。", true));
        announcementMapper.insert(new Announcement(
                "新生入馆须知",
                "欢迎新同学！凭学号可在首页自助注册读者账号，注册后即可在线检索书目、预约与续借。", true));
        announcementMapper.insert(new Announcement(
                "图书逾期提醒服务上线",
                "系统现已支持到期前3天自动推送提醒，避免产生滞纳金。请保持账号联系方式准确。", false));
        announcementMapper.insert(new Announcement(
                "读书月系列活动预告",
                "本月将举办“经典共读”“文献检索培训”等系列活动，详情请关注“读者活动”栏目。", false));
    }

    private void seedActivities() {
        if (activityMapper.selectCount(null) > 0) {
            return;
        }
        activityMapper.insert(new Activity(
                "读书月启动仪式", "年度读书月开幕，发布共读书单与打卡挑战，参与即有机会获得阅读礼包。",
                "校级", true));
        activityMapper.insert(new Activity(
                "文献检索技能培训", "图书馆员主讲：中外文数据库使用、核心期刊查找与参考文献管理工具实操。",
                "培训", false));
        activityMapper.insert(new Activity(
                "经典共读会 · 《百年孤独》", "师生共读拉美文学经典，分享阅读心得，现场设有自由讨论环节。",
                "沙龙", false));
        activityMapper.insert(new Activity(
                "亲子绘本故事会", "面向教职工子女的绘本讲读与手工活动，培养早期阅读兴趣。",
                "活动", false));
        activityMapper.insert(new Activity(
                "信息素养大赛", "以赛促学，提升学生检索、甄别与利用信息的能力，设校级奖项。",
                "竞赛", false));
    }

    private void seedBook(String isbn, String title, String author, String publisher, String category,
                          LocalDate publishDate, String language, BigDecimal price,
                          String description, int copyCount) {
        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        book.setPublisher(publisher);
        book.setCategory(category);
        book.setPublishDate(publishDate);
        book.setLanguage(language);
        book.setPrice(price);
        book.setDescription(description);
        bookMapper.insert(book);

        for (int i = 1; i <= copyCount; i++) {
            insertCopy(book, i);
        }
    }

    /**
     * 扩充书目：再补 10 本新书，副本数量随机（1–5 本）。
     *
     * <p>与 {@link #seedBooks()} 的「整表为空才种」不同，这里按 ISBN 逐本判重，
     * 老库（已有示例书）重启后也能把这 10 本补进去，且重复启动不会插重。
     */
    private void seedExtraBooks() {
        seedBookIfAbsent("978-7-5366-9293-0", "三体", "刘慈欣", "重庆出版社", "科幻",
                LocalDate.of(2008, 1, 1), "中文", new BigDecimal("23.00"),
                "中国科幻文学的里程碑之作，讲述地球文明与三体文明跨越数百年的博弈，"
                        + "获第 73 届雨果奖最佳长篇小说奖。");
        seedBookIfAbsent("978-7-5063-5435-8", "活着", "余华", "作家出版社", "文学",
                LocalDate.of(2012, 8, 1), "中文", new BigDecimal("28.00"),
                "讲述福贵一生历经家道败落、战乱与亲人离散的故事，以朴素的笔调写尽普通人"
                        + "在苦难中活下去的坚韧。");
        seedBookIfAbsent("978-7-5086-4735-7", "人类简史", "尤瓦尔·赫拉利", "中信出版社", "历史",
                LocalDate.of(2014, 11, 1), "中文", new BigDecimal("68.00"),
                "从认知革命、农业革命到科学革命，重新梳理人类十万年的发展脉络，"
                        + "探讨人类如何一步步成为地球的主宰。");
        seedBookIfAbsent("978-7-5357-3231-3", "时间简史", "史蒂芬·霍金", "湖南科学技术出版社", "科学",
                LocalDate.of(2010, 4, 1), "中文", new BigDecimal("45.00"),
                "霍金撰写的宇宙学科普经典，介绍宇宙起源、黑洞、时间箭头等前沿话题，"
                        + "让普通读者也能走近宇宙学。");
        seedBookIfAbsent("978-7-02-002475-9", "围城", "钱钟书", "人民文学出版社", "文学",
                LocalDate.of(1991, 2, 1), "中文", new BigDecimal("36.00"),
                "以留学生方鸿渐的经历为主线，用幽默机智的笔法描绘抗战初期知识分子的群像，"
                        + "「城外的人想冲进去，城里的人想逃出来」。");
        seedBookIfAbsent("978-7-111-54742-6", "Java核心技术 卷I", "凯·霍斯特曼", "机械工业出版社", "计算机",
                LocalDate.of(2016, 9, 1), "中文", new BigDecimal("149.00"),
                "Java 领域最有影响力的技术著作之一，系统讲解语言基础、面向对象、集合、"
                        + "Lambda 表达式与流库等核心特性。");
        seedBookIfAbsent("978-7-302-42328-7", "机器学习", "周志华", "清华大学出版社", "计算机",
                LocalDate.of(2016, 1, 1), "中文", new BigDecimal("88.00"),
                "国内机器学习课程的经典教材（俗称「西瓜书」），覆盖模型评估、线性模型、"
                        + "决策树、神经网络、支持向量机与集成学习等内容。");
        seedBookIfAbsent("978-7-301-24948-9", "经济学原理", "曼昆", "北京大学出版社", "经济",
                LocalDate.of(2015, 5, 1), "中文", new BigDecimal("128.00"),
                "经济学入门的经典教科书，以「十大原理」为纲，用贴近生活的案例讲解"
                        + "微观与宏观经济学的核心思想。");
        seedBookIfAbsent("978-7-5115-3586-9", "明朝那些事儿", "当年明月", "人民日报出版社", "历史",
                LocalDate.of(2017, 3, 1), "中文", new BigDecimal("358.00"),
                "以通俗幽默的网络语言讲述明朝三百年兴衰史，从朱元璋起兵到崇祯自缢，"
                        + "兼具史料的严谨与小说的可读性。");
        seedBookIfAbsent("978-7-1000-04293-7", "牛津高阶英汉双解词典", "霍恩比", "商务印书馆", "工具书",
                LocalDate.of(2018, 8, 1), "中文", new BigDecimal("169.00"),
                "全球销量领先的英语学习词典，收录 18 万余条单词与短语，释义严谨、"
                        + "例证丰富，是英语学习者案头必备的工具书。");
    }

    /** 按 ISBN 判重后补种一本书；副本条码沿用 C{bookId}-序号 规则，数量随机 1–5 */
    private void seedBookIfAbsent(String isbn, String title, String author, String publisher, String category,
                                  LocalDate publishDate, String language, BigDecimal price,
                                  String description) {
        if (bookMapper.selectCount(Wrappers.<Book>lambdaQuery().eq(Book::getIsbn, isbn)) > 0) {
            return;
        }
        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        book.setPublisher(publisher);
        book.setCategory(category);
        book.setPublishDate(publishDate);
        book.setLanguage(language);
        book.setPrice(price);
        book.setDescription(description);
        bookMapper.insert(book);

        int copyCount = ThreadLocalRandom.current().nextInt(1, 6);
        for (int i = 1; i <= copyCount; i++) {
            insertCopy(book, i);
        }
    }

    private void insertCopy(Book book, int seq) {
        BookCopy copy = new BookCopy();
        copy.setBookId(book.getId());
        copy.setBarcode("C" + book.getId() + String.format("-%03d", seq));
        copy.setLocation("A区" + ((book.getId() % 5) + 1) + "排");
        copyMapper.insert(copy);
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
