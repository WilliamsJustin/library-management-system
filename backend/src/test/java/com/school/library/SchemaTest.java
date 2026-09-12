package com.school.library;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.library.entity.Book;
import com.school.library.entity.BookCopy;
import com.school.library.entity.Loan;
import com.school.library.entity.LoanStatus;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderType;
import com.school.library.entity.UserRole;
import com.school.library.mapper.BookCopyMapper;
import com.school.library.mapper.BookMapper;
import com.school.library.mapper.LoanMapper;
import com.school.library.mapper.ReaderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SchemaTest {

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BookCopyMapper copyMapper;

    @Autowired
    private ReaderMapper readerMapper;

    @Autowired
    private LoanMapper loanMapper;

    @Test
    void testSchema() {
        // 创建书目
        Book book = new Book();
        book.setIsbn("978-7-302-12345-6");
        book.setTitle("测试书目");
        book.setAuthor("测试作者");
        book.setPublisher("测试出版社");
        book.setCategory("测试分类");
        bookMapper.insert(book);
        assertThat(book.getId()).isNotNull();

        // 创建副本
        BookCopy copy = new BookCopy();
        copy.setBookId(book.getId());
        copy.setBarcode("BARCODE-001");
        copy.setLocation("A区1排");
        copyMapper.insert(copy);

        // 创建读者
        Reader reader = new Reader();
        reader.setAccount("test-reader");
        reader.setPasswordHash("hash");
        reader.setName("测试读者");
        reader.setRole(UserRole.READER);
        reader.setType(ReaderType.STUDENT);
        reader.setStudentNo("STU-001");
        readerMapper.insert(reader);

        // 创建借阅记录
        Loan loan = new Loan();
        loan.setCopyId(copy.getId());
        loan.setReaderId(reader.getId());
        loan.setBorrowedAt(LocalDateTime.now().minusMinutes(5));
        loan.setDueDate(LocalDateTime.now().plusMinutes(10));
        loan.setRenewedCount(0);
        loan.setStatus(LoanStatus.ACTIVE);
        loanMapper.insert(loan);

        // 验证数据
        assertThat(bookMapper.selectById(book.getId())).isNotNull();
        assertThat(copyMapper.selectOne(Wrappers.<BookCopy>lambdaQuery()
                .eq(BookCopy::getBarcode, "BARCODE-001"))).isNotNull();
        assertThat(readerMapper.selectOne(Wrappers.<Reader>lambdaQuery()
                .eq(Reader::getAccount, "test-reader"))).isNotNull();
        assertThat(loanMapper.selectById(loan.getId())).isNotNull();

        // 验证外键关联（用 ID 关联，取代原来的实体导航）
        List<BookCopy> copies = copyMapper.selectList(Wrappers.<BookCopy>lambdaQuery()
                .eq(BookCopy::getBookId, book.getId()));
        assertThat(copies).hasSize(1);
        assertThat(copies.get(0).getBookId()).isEqualTo(book.getId());

        List<Loan> loans = loanMapper.selectList(Wrappers.<Loan>lambdaQuery()
                .eq(Loan::getReaderId, reader.getId()));
        assertThat(loans).hasSize(1);
        assertThat(loans.get(0).getReaderId()).isEqualTo(reader.getId());
    }
}
