package com.school.library.support;

import com.school.library.entity.*;
import com.school.library.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.context.TestComponent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 集成测试共用的数据构造工具 */
@TestComponent
public class TestData {

    public static final String DEFAULT_PASSWORD = "pass123";

    private final ReaderRepository readers;
    private final BookRepository books;
    private final BookCopyRepository copies;
    private final LoanRepository loans;
    private final PenaltyRepository penalties;
    private final PasswordEncoder passwordEncoder;

    public TestData(ReaderRepository readers, BookRepository books, BookCopyRepository copies,
                    LoanRepository loans, PenaltyRepository penalties, PasswordEncoder passwordEncoder) {
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
        return readers.save(r);
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
        return readers.save(r);
    }

    public Book book(String isbn, String title) {
        Book b = new Book();
        b.setIsbn(isbn);
        b.setTitle(title);
        b.setAuthor("作者" + isbn);
        b.setPublisher("出版社");
        b.setCategory("分类");
        return books.save(b);
    }

    public BookCopy copy(Book book, String barcode) {
        BookCopy c = new BookCopy();
        c.setBook(book);
        c.setBarcode(barcode);
        c.setLocation("A区1排");
        return copies.save(c);
    }

    public Loan activeLoan(Reader reader, BookCopy copy, LocalDate dueDate) {
        Loan l = new Loan();
        l.setCopy(copy);
        l.setReader(reader);
        l.setBorrowedAt(LocalDateTime.now().minusDays(10));
        l.setDueDate(dueDate);
        l.setRenewedCount(0);
        l.setStatus(LoanStatus.ACTIVE);
        copy.setStatus(CopyStatus.BORROWED);
        copies.save(copy);
        return loans.save(l);
    }

    public Penalty unpaidPenalty(Loan loan, BigDecimal amount) {
        Penalty p = new Penalty();
        p.setLoan(loan);
        p.setReader(loan.getReader());
        p.setAmount(amount);
        p.setStatus(PenaltyStatus.UNPAID);
        return penalties.save(p);
    }
}