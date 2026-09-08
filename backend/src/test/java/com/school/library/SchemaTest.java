package com.school.library;

import com.school.library.entity.*;
import com.school.library.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SchemaTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository copyRepository;

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Test
    void testSchema() {
        // 创建书目
        Book book = new Book();
        book.setIsbn("978-7-302-12345-6");
        book.setTitle("测试书目");
        book.setAuthor("测试作者");
        book.setPublisher("测试出版社");
        book.setCategory("测试分类");
        book = bookRepository.save(book);

        // 创建副本
        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setBarcode("BARCODE-001");
        copy.setLocation("A区1排");
        copy = copyRepository.save(copy);

        // 创建读者
        Reader reader = new Reader();
        reader.setAccount("test-reader");
        reader.setPasswordHash("hash");
        reader.setName("测试读者");
        reader.setRole(UserRole.READER);
        reader.setType(ReaderType.STUDENT);
        reader.setStudentNo("STU-001");
        reader = readerRepository.save(reader);

        // 创建借阅记录
        Loan loan = new Loan();
        loan.setCopy(copy);
        loan.setReader(reader);
        loan.setBorrowedAt(LocalDateTime.now().minusDays(10));
        loan.setDueDate(LocalDate.now().plusDays(20));
        loan.setRenewedCount(0);
        loan.setStatus(LoanStatus.ACTIVE);
        loan = loanRepository.save(loan);

        // 验证数据
        assertThat(bookRepository.findById(book.getId())).isPresent();
        assertThat(copyRepository.findByBarcode("BARCODE-001")).isPresent();
        assertThat(readerRepository.findByAccount("test-reader")).isPresent();
        assertThat(loanRepository.findById(loan.getId())).isPresent();

        // 验证关系
        List<BookCopy> copies = copyRepository.findByBookId(book.getId());
        assertThat(copies).hasSize(1);
        assertThat(copies.get(0).getBook().getId()).isEqualTo(book.getId());

        List<Loan> loans = loanRepository.findByReaderId(reader.getId());
        assertThat(loans).hasSize(1);
        assertThat(loans.get(0).getReader().getId()).isEqualTo(reader.getId());
    }
}