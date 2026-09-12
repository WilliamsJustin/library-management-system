package com.school.library;

import com.school.library.dto.BorrowRequest;
import com.school.library.dto.LoanResponse;
import com.school.library.entity.BookCopy;
import com.school.library.entity.CopyStatus;
import com.school.library.entity.Loan;
import com.school.library.entity.LoanStatus;
import com.school.library.entity.Notification;
import com.school.library.entity.Penalty;
import com.school.library.entity.Reader;
import com.school.library.exception.BusinessException;
import com.school.library.mapper.BookCopyMapper;
import com.school.library.mapper.LoanMapper;
import com.school.library.mapper.NotificationMapper;
import com.school.library.mapper.PenaltyMapper;
import com.school.library.mapper.ReaderMapper;
import com.school.library.security.AppPrincipal;
import com.school.library.service.LoanService;
import com.school.library.service.PenaltyService;
import com.school.library.support.TestData;
import com.school.library.task.OverdueTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 借阅流通 + 逾期罚款核心流程。
 *
 * <p>借期单位已由「天」改为「分钟」：学生/教师借期均为 10 分钟，
 * 逾期罚款 0.10 元/分钟 × 逾期分钟数，到期前 5 分钟提醒一次。
 */
@SpringBootTest
@Transactional
@Import(TestData.class)
class LoanFlowTest {

    /** 学生/教师统一借期（分钟），与 application.yml 的 app.library.loan-minutes 一致 */
    private static final int LOAN_MINUTES = 10;

    @Autowired
    private LoanService loanService;

    @Autowired
    private PenaltyService penaltyService;

    @Autowired
    private OverdueTask overdueTask;

    @Autowired
    private TestData testData;

    @Autowired
    private LoanMapper loanMapper;

    @Autowired
    private BookCopyMapper copyMapper;

    @Autowired
    private ReaderMapper readerMapper;

    @Autowired
    private PenaltyMapper penaltyMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    private AppPrincipal principal(Reader reader) {
        return new AppPrincipal(reader.getId(), reader.getAccount(),
                reader.getRole(), reader.getType().name());
    }

    /**
     * 构造一笔在借记录（副本同步置为已借出）。
     *
     * @param dueMinutesFromNow 应还时间相对当前时刻的分钟偏移，负数表示已逾期
     */
    private Loan activeLoan(Reader reader, BookCopy copy, long dueMinutesFromNow, LoanStatus status) {
        copy.setStatus(CopyStatus.BORROWED);
        copyMapper.updateById(copy);

        Loan loan = new Loan();
        loan.setCopyId(copy.getId());
        loan.setReaderId(reader.getId());
        loan.setBorrowedAt(LocalDateTime.now().minusMinutes(LOAN_MINUTES));
        loan.setDueDate(LocalDateTime.now().plusMinutes(dueMinutesFromNow));
        loan.setRenewedCount(0);
        loan.setStatus(status);
        loanMapper.insert(loan);
        return loan;
    }

    private List<Notification> notificationsOf(Reader reader) {
        return notificationMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<Notification>lambdaQuery()
                        .eq(Notification::getReaderId, reader.getId()));
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
        // 借期 10 分钟：应还时间应落在「现在 + 10 分钟」附近（留 2 分钟余量避免临界抖动）
        assertThat(response.getDueDate()).isBetween(
                LocalDateTime.now().plusMinutes(LOAN_MINUTES - 1),
                LocalDateTime.now().plusMinutes(LOAN_MINUTES + 1));
        assertThat(copyMapper.selectById(copy.getId()).getStatus().name()).isEqualTo("BORROWED");
    }

    /** 教师借期与学生一致，也是 10 分钟 */
    @Test
    void teacherLoanPeriodIsAlsoTenMinutes() {
        Reader teacher = testData.teacher("t-borrow");
        var book = testData.book("978-BORROW-T", "教师借阅测试书");
        BookCopy copy = testData.copy(book, "BAR-BORROW-T");

        BorrowRequest request = new BorrowRequest();
        request.setReaderId(teacher.getId());
        request.setCopyId(copy.getId());

        LoanResponse response = loanService.borrow(request);

        assertThat(response.getDueDate()).isBetween(
                LocalDateTime.now().plusMinutes(LOAN_MINUTES - 1),
                LocalDateTime.now().plusMinutes(LOAN_MINUTES + 1));
    }

    @Test
    void studentBorrowLimitIsFive() {
        Reader student = testData.student("s-limit");
        var book = testData.book("978-LIMIT-1", "限借测试书");

        for (int i = 1; i <= 5; i++) {
            BookCopy copy = testData.copy(book, "BAR-LIMIT-" + i);
            Loan loan = new Loan();
            loan.setCopyId(copy.getId());
            loan.setReaderId(student.getId());
            loan.setBorrowedAt(LocalDateTime.now().minusMinutes(1));
            loan.setDueDate(LocalDateTime.now().plusMinutes(LOAN_MINUTES));
            loan.setStatus(LoanStatus.ACTIVE);
            copy.setStatus(CopyStatus.BORROWED);
            copyMapper.updateById(copy);
            loanMapper.insert(loan);
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

        // 第一次续借成功，借期顺延 10 分钟
        LoanResponse renewed = loanService.renewLoan(loan.getId(), principal(student));
        assertThat(renewed.getRenewedCount()).isEqualTo(1);
        assertThat(renewed.getDueDate()).isEqualTo(loan.getDueDate().plusMinutes(LOAN_MINUTES));

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

        // 构造一笔已逾期 5 分钟的在借记录 → 罚款 5 × 0.10 = 0.50 元
        Loan loan = activeLoan(student, copy, -5, LoanStatus.ACTIVE);

        LoanResponse response = loanService.returnLoan(loan.getId());
        assertThat(response.getStatus().name()).isEqualTo("RETURNED");
        assertThat(copyMapper.selectById(copy.getId()).getStatus().name()).isEqualTo("IN_STOCK");

        // 逾期归还生成罚款，读者被限制借阅
        List<Penalty> penalties = penaltyMapper.selectList(null);
        assertThat(penalties).hasSize(1);
        assertThat(penalties.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("0.50"));
        assertThat(readerMapper.selectById(student.getId()).getStatus().name()).isEqualTo("RESTRICTED");

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
        assertThat(readerMapper.selectById(student.getId()).getStatus().name()).isEqualTo("NORMAL");
    }

    @Test
    void overdueLoanCannotBeRenewed() {
        Reader student = testData.student("s-overdue");
        var book = testData.book("978-OVERDUE-1", "逾期测试书");
        BookCopy copy = testData.copy(book, "BAR-OVERDUE-1");

        Loan savedLoan = activeLoan(student, copy, -5, LoanStatus.ACTIVE);

        assertThatThrownBy(() -> loanService.renewLoan(savedLoan.getId(), principal(student)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("逾期");
    }

    /** 罚款单价是「元/分钟」：逾期 12 分钟 = 1.20 元（而不是 0.10 × 1 天） */
    @Test
    void fineIsChargedPerMinute() {
        Reader student = testData.student("s-fine-minute");
        var book = testData.book("978-FINE-1", "按分钟罚款测试书");
        BookCopy copy = testData.copy(book, "BAR-FINE-1");

        Loan loan = activeLoan(student, copy, -12, LoanStatus.ACTIVE);
        penaltyService.settleOverdue(loan);

        List<Penalty> penalties = penaltyMapper.selectList(null);
        assertThat(penalties).hasSize(1);
        assertThat(penalties.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("1.20"));
        // 提醒文案按分钟描述
        assertThat(notificationsOf(student))
                .anySatisfy(n -> assertThat(n.getContent()).contains("分钟").contains("1.20"));
    }

    /** 罚款金额随逾期时长增长：重复结算同一借阅时金额重算，而不是停在首次的 0.10 元 */
    @Test
    void fineGrowsWhileLoanStaysOverdue() {
        Reader student = testData.student("s-fine-grow");
        var book = testData.book("978-FINE-2", "罚款增长测试书");
        BookCopy copy = testData.copy(book, "BAR-FINE-2");

        // 先只逾期 30 秒（不足 1 分钟按 1 分钟计 → 0.10 元）
        Loan loan = activeLoan(student, copy, 0, LoanStatus.OVERDUE);
        loan.setDueDate(LocalDateTime.now().minusSeconds(30));
        loanMapper.updateById(loan);
        penaltyService.settleOverdue(loan);

        Penalty penalty = penaltyMapper.selectList(null).get(0);
        assertThat(penalty.getAmount()).isEqualByComparingTo(new BigDecimal("0.10"));

        // 逾期时间拉长到 12 分钟后再次结算（模拟下一轮定时扫描）：金额应涨到 1.20 元
        loan.setDueDate(LocalDateTime.now().minusMinutes(12));
        loanMapper.updateById(loan);
        penaltyService.settleOverdue(loan);

        Penalty updated = penaltyMapper.selectList(null).get(0);
        assertThat(updated.getId()).isEqualTo(penalty.getId());
        assertThat(updated.getAmount()).isEqualByComparingTo(new BigDecimal("1.20"));
    }

    /** 罚金封顶：单本图书累计罚金到 144 元（1440 分钟）后不再累加 */
    @Test
    void fineIsCappedAtOneDay() {
        Reader student = testData.student("s-fine-cap");
        var book = testData.book("978-FINE-CAP", "罚金封顶测试书");
        BookCopy copy = testData.copy(book, "BAR-FINE-CAP");

        // 已逾期 2000 分钟（超过 1440 分钟封顶线）
        Loan loan = activeLoan(student, copy, -2000, LoanStatus.OVERDUE);
        penaltyService.settleOverdue(loan);

        List<Penalty> penalties = penaltyMapper.selectList(null);
        assertThat(penalties).hasSize(1);
        assertThat(penalties.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("144.00"));

        // 再结算一轮（逾期更久）：金额仍封顶在 144 元，不再增长
        penaltyService.settleOverdue(loan);
        assertThat(penaltyMapper.selectList(null).get(0).getAmount())
                .isEqualByComparingTo(new BigDecimal("144.00"));
    }

    /** 到期前 5 分钟提醒一次：应还时间在窗口内才提醒，且同一条借阅重复扫描不会重复提醒 */
    @Test
    void dueSoonReminderIsSentOnce() {
        Reader student = testData.student("s-remind");
        var book = testData.book("978-REMIND-1", "到期提醒测试书");
        BookCopy copy = testData.copy(book, "BAR-REMIND-1");

        // 还有 3 分钟到期（落在「到期前 5 分钟」窗口内）
        Loan loan = activeLoan(student, copy, 3, LoanStatus.ACTIVE);
        assertThat(loan.isDueReminderSent()).isFalse();

        overdueTask.checkOnce();
        List<Notification> afterFirst = notificationsOf(student);
        assertThat(afterFirst).hasSize(1);
        assertThat(afterFirst.get(0).getContent()).contains("还剩").contains("分钟");
        assertThat(loanMapper.selectById(loan.getId()).isDueReminderSent()).isTrue();

        // 再扫一轮：不应重复下发
        overdueTask.checkOnce();
        assertThat(notificationsOf(student)).hasSize(1);
        assertThat(loanMapper.selectById(loan.getId()).isDueReminderSent()).isTrue();
    }

    /** 距到期还很远（10 分钟借期刚借出）不应提前提醒 */
    @Test
    void reminderNotSentWhenDueIsFarAway() {
        Reader student = testData.student("s-noremind");
        var book = testData.book("978-REMIND-2", "未到期提醒测试书");
        BookCopy copy = testData.copy(book, "BAR-REMIND-2");

        activeLoan(student, copy, LOAN_MINUTES, LoanStatus.ACTIVE);
        overdueTask.checkOnce();

        assertThat(notificationsOf(student)).isEmpty();
    }

    /** 到期未还：定时任务把记录标为逾期并生成罚款 */
    @Test
    void scheduledCheckMarksOverdueAndCreatesPenalty() {
        Reader student = testData.student("s-task-overdue");
        var book = testData.book("978-TASK-1", "定时逾期测试书");
        BookCopy copy = testData.copy(book, "BAR-TASK-1");

        // 已逾期 3 分钟
        Loan loan = activeLoan(student, copy, -3, LoanStatus.ACTIVE);

        overdueTask.checkOnce();

        assertThat(loanMapper.selectById(loan.getId()).getStatus()).isEqualTo(LoanStatus.OVERDUE);
        List<Penalty> penalties = penaltyMapper.selectList(null);
        assertThat(penalties).hasSize(1);
        assertThat(penalties.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("0.30"));
        assertThat(readerMapper.selectById(student.getId()).getStatus().name()).isEqualTo("RESTRICTED");
    }
}
