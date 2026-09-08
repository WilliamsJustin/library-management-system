package com.school.library.service.impl;

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
import com.school.library.repository.NotificationRepository;
import com.school.library.repository.PenaltyRepository;
import com.school.library.repository.ReaderRepository;
import com.school.library.security.AppPrincipal;
import com.school.library.service.CirculationPolicy;
import com.school.library.service.PenaltyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class PenaltyServiceImpl implements PenaltyService {

    @Autowired
    private PenaltyRepository penaltyRepository;

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CirculationPolicy policy;

    @Override
    @Transactional(readOnly = true)
    public Page<PenaltyResponse> getPenalties(PenaltyStatus status, Pageable pageable) {
        return penaltyRepository.query(status, pageable)
                .map(PenaltyResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PenaltyResponse> getMyPenalties(AppPrincipal caller, Pageable pageable) {
        return penaltyRepository.findByReaderIdOrderByIdDesc(caller.userId(), pageable)
                .map(PenaltyResponse::fromEntity);
    }

    @Override
    @Transactional
    public void payPenalty(Long id, AppPrincipal caller) {
        Penalty penalty = penaltyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("罚款记录不存在"));

        // 读者只能缴纳自己的罚款
        if (caller.role() != UserRole.ADMIN
                && !penalty.getReader().getId().equals(caller.userId())) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "只能缴纳自己的罚款");
        }

        if (penalty.getStatus() == PenaltyStatus.PAID) {
            throw new ConflictException("该罚款已缴纳");
        }

        penalty.setStatus(PenaltyStatus.PAID);
        penalty.setPaidAt(java.time.LocalDateTime.now());
        penaltyRepository.save(penalty);

        // 缴清全部罚款后自动恢复借阅资格
        if (penaltyRepository.countByReaderIdAndStatus(penalty.getReader().getId(),
                PenaltyStatus.UNPAID) == 0) {
            Reader reader = penalty.getReader();
            if (reader.getStatus() == ReaderStatus.RESTRICTED) {
                reader.setStatus(ReaderStatus.NORMAL);
                readerRepository.save(reader);
            }
            notificationRepository.save(new Notification(reader.getId(),
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
        if (penaltyRepository.existsByLoanId(loan.getId())) {
            return;
        }

        long overdueDays = Math.max(ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now()), 1);
        BigDecimal amount = policy.finePerDay().multiply(BigDecimal.valueOf(overdueDays));

        Penalty penalty = new Penalty();
        penalty.setLoan(loan);
        penalty.setReader(loan.getReader());
        penalty.setAmount(amount);
        penalty.setStatus(PenaltyStatus.UNPAID);
        penaltyRepository.save(penalty);

        // 限制借阅资格
        Reader reader = loan.getReader();
        if (reader.getStatus() == ReaderStatus.NORMAL) {
            reader.setStatus(ReaderStatus.RESTRICTED);
            readerRepository.save(reader);
        }

        notificationRepository.save(new Notification(reader.getId(),
                "您借阅的《" + loan.getCopy().getBook().getTitle() + "》已逾期 " + overdueDays
                        + " 天，产生罚款 " + amount + " 元，请及时归还并缴纳罚款。"));
    }
}
