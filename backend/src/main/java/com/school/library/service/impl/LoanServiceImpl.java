package com.school.library.service.impl;

import com.school.library.dto.BorrowRequest;
import com.school.library.dto.LoanResponse;
import com.school.library.entity.BookCopy;
import com.school.library.entity.BookStatus;
import com.school.library.entity.CopyStatus;
import com.school.library.entity.Loan;
import com.school.library.entity.LoanStatus;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderStatus;
import com.school.library.entity.UserRole;
import com.school.library.exception.BusinessException;
import com.school.library.exception.ErrorCodes;
import com.school.library.exception.NotFoundException;
import com.school.library.repository.BookCopyRepository;
import com.school.library.repository.LoanRepository;
import com.school.library.repository.PenaltyRepository;
import com.school.library.repository.ReaderRepository;
import com.school.library.security.AppPrincipal;
import com.school.library.service.CirculationPolicy;
import com.school.library.service.LoanService;
import com.school.library.service.PenaltyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LoanServiceImpl implements LoanService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookCopyRepository copyRepository;

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private PenaltyRepository penaltyRepository;

    @Autowired
    private CirculationPolicy policy;

    @Autowired
    private PenaltyService penaltyService;

    @Override
    @Transactional
    public LoanResponse borrow(BorrowRequest request) {
        Reader reader = resolveReader(request);
        BookCopy copy = resolveCopy(request);
        return doBorrow(reader, copy);
    }

    @Override
    @Transactional
    public LoanResponse borrowSelf(AppPrincipal caller, BorrowRequest request) {
        Reader reader = readerRepository.findById(caller.userId())
                .orElseThrow(() -> new NotFoundException("读者不存在"));
        BookCopy copy = resolveCopy(request);
        return doBorrow(reader, copy);
    }

    private LoanResponse doBorrow(Reader reader, BookCopy copy) {
        // 读者状态检查：受限（停借）不可借阅
        if (reader.getStatus() != ReaderStatus.NORMAL) {
            throw new BusinessException(ErrorCodes.READER_RESTRICTED, "读者当前处于停借状态，不可借阅");
        }

        // 有未缴罚金不可借阅
        if (penaltyRepository.countByReaderIdAndStatus(reader.getId(),
                com.school.library.entity.PenaltyStatus.UNPAID) > 0) {
            throw new BusinessException(ErrorCodes.READER_RESTRICTED, "读者有未缴罚金，请先缴纳后再借阅");
        }

        // 借阅数量限制：学生 5 本，教师 10 本
        long activeCount = loanRepository.countByReaderIdAndStatusIn(reader.getId(),
                List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE));
        int limit = policy.maxBorrowCount(reader.getType());
        if (activeCount >= limit) {
            throw new BusinessException(ErrorCodes.BORROW_LIMIT_EXCEEDED,
                    "超出可借数量上限（" + (reader.getType()) + "最多可借 " + limit + " 本）");
        }

        // 副本状态检查
        if (copy.getStatus() == CopyStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCodes.WITHDRAW_NOT_ALLOWED, "该副本已下架，不可借阅");
        }
        if (copy.getStatus() != CopyStatus.IN_STOCK) {
            throw new BusinessException(ErrorCodes.COPY_NOT_AVAILABLE, "该副本已被借出，不可借阅");
        }
        if (copy.getBook().getStatus() == BookStatus.INACTIVE) {
            throw new BusinessException(ErrorCodes.COPY_NOT_AVAILABLE, "该图书已下架，不可借阅");
        }

        Loan loan = new Loan();
        loan.setCopy(copy);
        loan.setReader(reader);
        loan.setBorrowedAt(LocalDateTime.now());
        loan.setDueDate(LocalDate.now().plusDays(policy.loanDays(reader.getType())));
        loan.setRenewedCount(0);
        loan.setStatus(LoanStatus.ACTIVE);

        copy.setStatus(CopyStatus.BORROWED);
        copyRepository.save(copy);

        return LoanResponse.fromEntity(loanRepository.save(loan));
    }

    @Override
    @Transactional
    public LoanResponse returnLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("借阅记录不存在"));
        return doReturn(loan);
    }

    @Override
    @Transactional
    public LoanResponse returnSelf(AppPrincipal caller, Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("借阅记录不存在"));

        // 读者只能归还自己借阅的图书，管理员不受此限
        if (caller.role() != UserRole.ADMIN
                && !loan.getReader().getId().equals(caller.userId())) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "只能归还自己借阅的图书");
        }

        return doReturn(loan);
    }

    private LoanResponse doReturn(Loan loan) {
        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException(ErrorCodes.LOAN_NOT_ACTIVE, "该图书已归还");
        }

        // 逾期归还得兜底生成罚款（定时任务未跑到时保证幂等）
        penaltyService.settleOverdue(loan);

        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnedAt(LocalDateTime.now());
        loan.getCopy().setStatus(CopyStatus.IN_STOCK);
        copyRepository.save(loan.getCopy());

        return LoanResponse.fromEntity(loanRepository.save(loan));
    }

    @Override
    @Transactional
    public LoanResponse renewLoan(Long loanId, AppPrincipal caller) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("借阅记录不存在"));

        // 读者只能续借自己的图书
        if (caller.role() != UserRole.ADMIN
                && !loan.getReader().getId().equals(caller.userId())) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "只能续借自己的图书");
        }

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException(ErrorCodes.LOAN_NOT_ACTIVE, "该图书已归还，无法续借");
        }
        if (loan.getStatus() == LoanStatus.OVERDUE || loan.getDueDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCodes.ALREADY_OVERDUE, "该图书已逾期，无法续借，请先归还并缴纳罚款");
        }
        if (loan.getRenewedCount() >= policy.maxRenewCount()) {
            throw new BusinessException(ErrorCodes.RENEWAL_NOT_ALLOWED,
                    "每本图书最多续借 " + policy.maxRenewCount() + " 次");
        }

        loan.setRenewedCount(loan.getRenewedCount() + 1);
        loan.setDueDate(loan.getDueDate().plusDays(policy.loanDays(loan.getReader().getType())));

        return LoanResponse.fromEntity(loanRepository.save(loan));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanResponse> getLoans(Long readerId, LoanStatus status, Pageable pageable) {
        return loanRepository.query(readerId, status, pageable)
                .map(LoanResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanResponse> getMyLoans(AppPrincipal caller, Pageable pageable) {
        return loanRepository.findByReaderIdOrderByIdDesc(caller.userId(), pageable)
                .map(LoanResponse::fromEntity);
    }

    private Reader resolveReader(BorrowRequest request) {
        if (request.getReaderId() != null) {
            return readerRepository.findById(request.getReaderId())
                    .orElseThrow(() -> new NotFoundException("读者不存在"));
        }
        if (request.getReaderAccount() != null && !request.getReaderAccount().isBlank()) {
            return readerRepository.findByAccount(request.getReaderAccount())
                    .orElseThrow(() -> new NotFoundException("读者不存在"));
        }
        throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "请指定读者（readerId 或 readerAccount）");
    }

    private BookCopy resolveCopy(BorrowRequest request) {
        if (request.getCopyId() != null) {
            return copyRepository.findById(request.getCopyId())
                    .orElseThrow(() -> new NotFoundException("副本不存在"));
        }
        if (request.getBarcode() != null && !request.getBarcode().isBlank()) {
            Optional<BookCopy> copy = copyRepository.findByBarcode(request.getBarcode());
            if (copy.isEmpty()) {
                throw new NotFoundException("副本不存在");
            }
            return copy.get();
        }
        throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "请指定副本（copyId 或 barcode）");
    }
}
