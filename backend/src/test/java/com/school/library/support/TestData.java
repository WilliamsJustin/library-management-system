package com.school.library.support;

import com.school.library.entity.Book;
import com.school.library.entity.BookCopy;
import com.school.library.entity.CopyStatus;
import com.school.library.entity.Loan;
import com.school.library.entity.LoanStatus;
import com.school.library.entity.Penalty;
import com.school.library.entity.PenaltyStatus;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderType;
import com.school.library.entity.UserRole;
import com.school.library.mapper.BookCopyMapper;
import com.school.library.mapper.BookMapper;
import com.school.library.mapper.LoanMapper;
import com.school.library.mapper.PenaltyMapper;
import com.school.library.mapper.ReaderMapper;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 集成测试共用的数据构造工具（持久层已由 Repository 换成 MyBatis-Plus 的 Mapper） */
@TestComponent
public class TestData {

    public static final String DEFAULT_PASSWORD = "pass123";

    private final ReaderMapper readers;
    private final BookMapper books;
    private final BookCopyMapper copies;
    private final LoanMapper loans;
    private final PenaltyMapper penalties;
    private final PasswordEncoder passwordEncoder;

    public TestData(ReaderMapper readers, BookMapper books, BookCopyMapper copies,
                    LoanMapper loans, PenaltyMapper penalties, PasswordEncoder passwordEncoder) {
        this.readers = readers;
        this.books = books;
        this.copies = copies;
        this.loans = loans;
        this.penalties = penalties;
        this.passwordEncoder = passwordEncoder;
    }

    public Reader admin(String account) {
        Reader r = new Reader();
        r.setAccount(account);
        r.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        r.setName("管理员" + account);
        r.setRole(UserRole.ADMIN);
        r.setType(ReaderType.TEACHER);
        r.setStudentNo("STAFF-" + account);
        readers.insert(r);
        return r;
    }

    public Reader student(String account) {
        return reader(account, ReaderType.STUDENT);
    }

    public Reader teacher(String account) {
        return reader(account, ReaderType.TEACHER);
    }

    public Reader reader(String account, ReaderType type) {
        Reader r = new Reader();
        r.setAccount(account);
        r.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        r.setName("读者" + account);
        r.setRole(UserRole.READER);
        r.setType(type);
        r.setStudentNo("NO-" + account);
        readers.insert(r);
        return r;
    }

    public Book book(String isbn, String title) {
        Book b = new Book();
        b.setIsbn(isbn);
        b.setTitle(title);
        b.setAuthor("作者" + isbn);
        b.setPublisher("出版社");
        b.setCategory("分类");
        books.insert(b);
        return b;
    }

    public BookCopy copy(Book book, String barcode) {
        BookCopy c = new BookCopy();
        c.setBookId(book.getId());
        c.setBarcode(barcode);
        c.setLocation("A区1排");
        copies.insert(c);
        return c;
    }

    public Loan activeLoan(Reader reader, BookCopy copy, LocalDate dueDate) {
        Loan l = new Loan();
        l.setCopyId(copy.getId());
        l.setReaderId(reader.getId());
        l.setBorrowedAt(LocalDateTime.now().minusDays(10));
        l.setDueDate(dueDate);
        l.setRenewedCount(0);
        l.setStatus(LoanStatus.ACTIVE);
        copy.setStatus(CopyStatus.BORROWED);
        copies.updateById(copy);
        loans.insert(l);
        return l;
    }

    public Penalty unpaidPenalty(Loan loan, BigDecimal amount) {
        Penalty p = new Penalty();
        p.setLoanId(loan.getId());
        p.setReaderId(loan.getReaderId());
        p.setAmount(amount);
        p.setStatus(PenaltyStatus.UNPAID);
        penalties.insert(p);
        return p;
    }
}
