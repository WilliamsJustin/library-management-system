package com.school.library;

import com.school.library.dto.BorrowRequest;
import com.school.library.dto.LoanResponse;
import com.school.library.entity.BookCopy;
import com.school.library.entity.Loan;
import com.school.library.entity.Penalty;
import com.school.library.entity.Reader;
import com.school.library.exception.BusinessException;
import com.school.library.repository.BookCopyRepository;
import com.school.library.repository.LoanRepository;
import com.school.library.repository.PenaltyRepository;
import com.school.library.repository.ReaderRepository;
import com.school.library.security.AppPrincipal;
import com.school.library.service.LoanService;
import com.school.library.service.PenaltyService;
import com.school.library.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 借阅流通 + 逾期罚款核心流程 */
@SpringBootTest
@Transactional
@Import(TestData.class)
class LoanFlowTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private PenaltyService penaltyService;

    @Autowired
    private TestData testData;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookCopyRepository copyRepository;

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private PenaltyRepository penaltyRepository;

    private AppPrincipal principal(Reader reader) {
        return new AppPrincipal(reader.getId(), reader.getAccount(),
                reader.getRole(), reader.getType().name());
    }

    @Test
    void borrowSuccess() {
        Reader student = testData.student("s-borrow");
        var book = testData.book("978-BORROW-1", "借阅测试书");
        BookCopy copy = testData.copy(book, "BAR-BORROW-1");

        BorrowRequest request = new BorrowRequest();
        request.setReaderId(student.getId());
        request.setCopyId(copy.getId());

        LoanResponse response = loanService.borrow(request);

        assertThat(response.getStatus().name()).isEqualTo("ACTIVE");
        assertThat(response.getDueDate()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(copyRepository.findById(copy.getId()).orElseThrow().getStatus().name())
                .isEqualTo("BORROWED");
    }

    @Test
    void studentBorrowLimitIsFive() {
        Reader student = testData.student("s-limit");
        var book = testData.book("978-LIMIT-1", "限借测试书");

        for (int i = 1; i <= 5; i++) {
            BookCopy copy = testData.copy(book, "BAR-LIMIT-" + i);
            Loan loan = new Loan();
            loan.setCopy(copy);
            loan.setReader(student);
            loan.setBorrowedAt(java.time.LocalDateTime.now().minusDays(1));
            loan.setDueDate(LocalDate.now().plusDays(20));
            loan.setStatus(com.school.library.entity.LoanStatus.ACTIVE);
            copy.setStatus(com.school.library.entity.CopyStatus.BORROWED);
            copyRepository.save(copy);
            loanRepository.save(loan);
        }

        BookCopy sixth = testData.copy(book, "BAR-LIMIT-6");
        BorrowRequest request = new BorrowRequest();
        request.setReaderId(student.getId());
        request.setCopyId(sixth.getId());

        assertThatThrownBy(() -> loanService.borrow(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("上限");
    }

    @Test
    void borrowedCopyCannotBeBorrowedAgain() {
        Reader student = testData.student("s-conflict");
        var book = testData.book("978-CONFLICT-1", "冲突测试书");
        BookCopy copy = testData.copy(book, "BAR-CONFLICT-1");

        BorrowRequest request = new BorrowRequest();
        request.setReaderId(student.getId());
        request.setCopyId(copy.getId());
        loanService.borrow(request);

        Reader other = testData.student("s-conflict-2");
        request.setReaderId(other.getId());
        assertThatThrownBy(() -> loanService.borrow(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被借出");
    }

    @Test
    void renewOnceOnly() {
        Reader student = testData.student("s-renew");
        var book = testData.book("978-RENEW-1", "续借测试书");
        BookCopy copy = testData.copy(book, "BAR-RENEW-1");

        BorrowRequest request = new BorrowRequest();
        request.setReaderId(student.getId());
        request.setCopyId(copy.getId());
        LoanResponse loan = loanService.borrow(request);

        // 第一次续借成功，借期顺延 30 天
        LoanResponse renewed = loanService.renewLoan(loan.getId(), principal(student));
        assertThat(renewed.getRenewedCount()).isEqualTo(1);
        assertThat(renewed.getDueDate()).isEqualTo(loan.getDueDate().plusDays(30));

        // 第二次续借被拒绝
        assertThatThrownBy(() -> loanService.renewLoan(loan.getId(), principal(student)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("续借");
    }

    @Test
    void returnRestoresCopyAndOverdueCreatesPenalty() {
        Reader student = testData.student("s-return");
        var book = testData.book("978-RETURN-1", "归还测试书");
        BookCopy copy = testData.copy(book, "BAR-RETURN-1");

        // 构造一笔已逾期的在借记录
        Loan loan = new Loan();
        loan.setCopy(copy);
        loan.setReader(student);
        loan.setBorrowedAt(java.time.LocalDateTime.now().minusDays(35));
        loan.setDueDate(LocalDate.now().minusDays(5));
        loan.setStatus(com.school.library.entity.LoanStatus.OVERDUE);
        copy.setStatus(com.school.library.entity.CopyStatus.BORROWED);
        copyRepository.save(copy);
        loan = loanRepository.save(loan);

        LoanResponse response = loanService.returnLoan(loan.getId());
        assertThat(response.getStatus().name()).isEqualTo("RETURNED");
        assertThat(copyRepository.findById(copy.getId()).orElseThrow().getStatus().name())
                .isEqualTo("IN_STOCK");

        // 逾期归还生成罚款，读者被限制借阅
        List<Penalty> penalties = penaltyRepository.findAll();
        assertThat(penalties).hasSize(1);
        assertThat(penalties.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("0.50"));
        assertThat(readerRepository.findById(student.getId()).orElseThrow().getStatus().name())
                .isEqualTo("RESTRICTED");

        // 读者被限制借阅后不可再借
        BookCopy another = testData.copy(book, "BAR-RETURN-2");
        BorrowRequest request = new BorrowRequest();
        request.setReaderId(student.getId());
        request.setCopyId(another.getId());
        assertThatThrownBy(() -> loanService.borrow(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("停借");

        // 缴清罚款后恢复借阅资格
        penaltyService.payPenalty(penalties.get(0).getId(), principal(student));
        assertThat(readerRepository.findById(student.getId()).orElseThrow().getStatus().name())
                .isEqualTo("NORMAL");
    }

    @Test
    void overdueLoanCannotBeRenewed() {
        Reader student = testData.student("s-overdue");
        var book = testData.book("978-OVERDUE-1", "逾期测试书");
        BookCopy copy = testData.copy(book, "BAR-OVERDUE-1");

        Loan loan = new Loan();
        loan.setCopy(copy);
        loan.setReader(student);
        loan.setBorrowedAt(java.time.LocalDateTime.now().minusDays(35));
        loan.setDueDate(LocalDate.now().minusDays(5));
        loan.setStatus(com.school.library.entity.LoanStatus.OVERDUE);
        copy.setStatus(com.school.library.entity.CopyStatus.BORROWED);
        copyRepository.save(copy);
        Loan savedLoan = loanRepository.save(loan);

        assertThatThrownBy(() -> loanService.renewLoan(savedLoan.getId(), principal(student)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("逾期");
    }
}
