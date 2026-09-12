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
import com.school.library.entity.ReaderType;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
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
    public PageResult<PenaltyResponse> getPenalties(PenaltyStatus status, ReaderType readerType,
                                                    String keyword, int page, int size) {
        IPage<PenaltyResponse> result = penaltyMapper.selectDetailPage(Pages.of(page, size),
                null, status, readerType, keyword);
        return PageResult.of(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PenaltyResponse> getMyPenalties(AppPrincipal caller, PenaltyStatus status, String keyword,
                                                      String dateField, LocalDate startDate, LocalDate endDate,
                                                      int page, int size) {
        // 时间列白名单：CREATED 生成（默认）/ PAID 缴费，其余值一律按生成时间处理
        String field = "PAID".equals(dateField) ? "PAID" : "CREATED";
        // 日期闭区间换算成 [start, endExclusive) 半开区间，保证「当天」完整包含
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endExclusive = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        IPage<PenaltyResponse> result = penaltyMapper.selectMyDetailPage(Pages.of(page, size),
                caller.userId(), status, keyword, field, start, endExclusive);
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
        LocalDateTime now = LocalDateTime.now();
        // 未到期不处理
        if (!loan.getDueDate().isBefore(now)) {
            return;
        }
        // 罚款按分钟累计：0.10 元/分钟 × 逾期分钟数（不足 1 分钟按 1 分钟计），
        // 单本累计封顶 max-fine-minutes（1440 分钟 = 144 元），到顶后不再累加。
        // 每笔借阅即一本书，各自的罚款独立按自己的逾期时长累计。
        long overdueMinutes = Math.min(
                Math.max(ChronoUnit.MINUTES.between(loan.getDueDate(), now), 1),
                policy.maxFineMinutes());
        BigDecimal amount = policy.finePerMinute()
                .multiply(BigDecimal.valueOf(overdueMinutes))
                .setScale(2, RoundingMode.HALF_UP);

        // 幂等 + 金额随时间增长：同一借阅只生成一笔罚款（uk_penalty_loan 兜底），
        // 但借阅仍未归还时，每轮轮询都要把金额重算到当前逾期分钟数，
        // 否则罚款会永远停在首次检测时的 0.10 元。
        Penalty existing = penaltyMapper.selectOne(Wrappers.<Penalty>lambdaQuery()
                .eq(Penalty::getLoanId, loan.getId()));
        if (existing != null) {
            if (existing.getStatus() == PenaltyStatus.UNPAID
                    && existing.getAmount().compareTo(amount) < 0) {
                existing.setAmount(amount);
                penaltyMapper.updateById(existing);
            }
            return;
        }

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
                "您借阅的《" + bookTitle + "》已逾期 " + overdueMinutes
                        + " 分钟，产生罚款 " + amount + " 元，请及时归还并缴纳罚款。"));
    }

    private long countUnpaid(Long readerId) {
        return penaltyMapper.selectCount(Wrappers.<Penalty>lambdaQuery()
                .eq(Penalty::getReaderId, readerId)
                .eq(Penalty::getStatus, PenaltyStatus.UNPAID));
    }
}
