package com.school.library.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.library.common.PageResult;
import com.school.library.common.Pages;
import com.school.library.dto.PenaltyResponse;
import com.school.library.entity.Loan;
import com.school.library.entity.Notification;
import com.school.library.entity.Penalty;
import com.school.library.entity.PenaltyStatus;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderStatus;
import com.school.library.entity.UserRole;
import com.school.library.exception.BusinessException;
import com.school.library.exception.ConflictException;
import com.school.library.exception.ErrorCodes;
import com.school.library.exception.NotFoundException;
import com.school.library.mapper.LoanMapper;
import com.school.library.mapper.NotificationMapper;
import com.school.library.mapper.PenaltyMapper;
import com.school.library.mapper.ReaderMapper;
import com.school.library.service.CirculationPolicy;
import com.school.library.service.PenaltyService;
import com.school.library.security.AppPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class PenaltyServiceImpl implements PenaltyService {

    private final PenaltyMapper penaltyMapper;
    private final ReaderMapper readerMapper;
    private final NotificationMapper notificationMapper;
    private final LoanMapper loanMapper;
    private final CirculationPolicy policy;

    public PenaltyServiceImpl(PenaltyMapper penaltyMapper, ReaderMapper readerMapper,
                              NotificationMapper notificationMapper, LoanMapper loanMapper,
                              CirculationPolicy policy) {
        this.penaltyMapper = penaltyMapper;
        this.readerMapper = readerMapper;
        this.notificationMapper = notificationMapper;
        this.loanMapper = loanMapper;
        this.policy = policy;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PenaltyResponse> getPenalties(PenaltyStatus status, int page, int size) {
        IPage<PenaltyResponse> result = penaltyMapper.selectDetailPage(Pages.of(page, size), null, status);
        return PageResult.of(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PenaltyResponse> getMyPenalties(AppPrincipal caller, int page, int size) {
        IPage<PenaltyResponse> result = penaltyMapper.selectDetailPage(Pages.of(page, size), caller.userId(), null);
        return PageResult.of(result);
    }

    @Override
    @Transactional
    public void payPenalty(Long id, AppPrincipal caller) {
        Penalty penalty = penaltyMapper.selectById(id);
        if (penalty == null) {
            throw new NotFoundException("罚款记录不存在");
        }

        // 读者只能缴纳自己的罚款
        if (caller.role() != UserRole.ADMIN && !penalty.getReaderId().equals(caller.userId())) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "只能缴纳自己的罚款");
        }

        if (penalty.getStatus() == PenaltyStatus.PAID) {
            throw new ConflictException("该罚款已缴纳");
        }

        penalty.setStatus(PenaltyStatus.PAID);
        penalty.setPaidAt(LocalDateTime.now());
        penaltyMapper.updateById(penalty);

        // 缴清全部罚款后自动恢复借阅资格
        if (countUnpaid(penalty.getReaderId()) == 0) {
            Reader reader = readerMapper.selectById(penalty.getReaderId());
            if (reader != null && reader.getStatus() == ReaderStatus.RESTRICTED) {
                reader.setStatus(ReaderStatus.NORMAL);
                readerMapper.updateById(reader);
            }
            notificationMapper.insert(new Notification(penalty.getReaderId(),
                    "您的罚款已全部缴清，借阅资格已恢复。"));
        }
    }

    @Override
    @Transactional
    public void settleOverdue(Loan loan) {
        // 未到期不处理
        if (!loan.getDueDate().isBefore(LocalDate.now())) {
            return;
        }
        // 幂等：同一借阅只生成一笔罚款（uk_penalty_loan 兜底）
        if (penaltyMapper.selectCount(Wrappers.<Penalty>lambdaQuery()
                .eq(Penalty::getLoanId, loan.getId())) > 0) {
            return;
        }

        long overdueDays = Math.max(ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now()), 1);
        BigDecimal amount = policy.finePerDay().multiply(BigDecimal.valueOf(overdueDays));

        Penalty penalty = new Penalty();
        penalty.setLoanId(loan.getId());
        penalty.setReaderId(loan.getReaderId());
        penalty.setAmount(amount);
        penalty.setStatus(PenaltyStatus.UNPAID);
        penaltyMapper.insert(penalty);

        // 限制借阅资格
        Reader reader = readerMapper.selectById(loan.getReaderId());
        if (reader != null && reader.getStatus() == ReaderStatus.NORMAL) {
            reader.setStatus(ReaderStatus.RESTRICTED);
            readerMapper.updateById(reader);
        }

        String bookTitle = loanMapper.selectBookTitleByLoanId(loan.getId());
        notificationMapper.insert(new Notification(loan.getReaderId(),
                "您借阅的《" + bookTitle + "》已逾期 " + overdueDays
                        + " 天，产生罚款 " + amount + " 元，请及时归还并缴纳罚款。"));
    }

    private long countUnpaid(Long readerId) {
        return penaltyMapper.selectCount(Wrappers.<Penalty>lambdaQuery()
                .eq(Penalty::getReaderId, readerId)
                .eq(Penalty::getStatus, PenaltyStatus.UNPAID));
    }
}
