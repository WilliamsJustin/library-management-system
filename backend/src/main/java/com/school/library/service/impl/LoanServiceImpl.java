package com.school.library.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.library.common.PageResult;
import com.school.library.common.Pages;
import com.school.library.dto.BorrowRequest;
import com.school.library.dto.LoanResponse;
import com.school.library.entity.Book;
import com.school.library.entity.BookCopy;
import com.school.library.entity.BookStatus;
import com.school.library.entity.CopyStatus;
import com.school.library.entity.Loan;
import com.school.library.entity.LoanStatus;
import com.school.library.entity.Penalty;
import com.school.library.entity.PenaltyStatus;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderStatus;
import com.school.library.entity.UserRole;
import com.school.library.exception.BusinessException;
import com.school.library.exception.ConflictException;
import com.school.library.exception.ErrorCodes;
import com.school.library.exception.NotFoundException;
import com.school.library.mapper.BookCopyMapper;
import com.school.library.mapper.BookMapper;
import com.school.library.mapper.LoanMapper;
import com.school.library.mapper.PenaltyMapper;
import com.school.library.mapper.ReaderMapper;
import com.school.library.security.AppPrincipal;
import com.school.library.service.CirculationPolicy;
import com.school.library.service.LoanService;
import com.school.library.service.PenaltyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoanServiceImpl implements LoanService {

    private final LoanMapper loanMapper;
    private final BookCopyMapper copyMapper;
    private final BookMapper bookMapper;
    private final ReaderMapper readerMapper;
    private final PenaltyMapper penaltyMapper;
    private final CirculationPolicy policy;
    private final PenaltyService penaltyService;

    public LoanServiceImpl(LoanMapper loanMapper, BookCopyMapper copyMapper, BookMapper bookMapper,
                           ReaderMapper readerMapper, PenaltyMapper penaltyMapper,
                           CirculationPolicy policy, PenaltyService penaltyService) {
        this.loanMapper = loanMapper;
        this.copyMapper = copyMapper;
        this.bookMapper = bookMapper;
        this.readerMapper = readerMapper;
        this.penaltyMapper = penaltyMapper;
        this.policy = policy;
        this.penaltyService = penaltyService;
    }

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
        Reader reader = readerMapper.selectById(caller.userId());
        if (reader == null) {
            throw new NotFoundException("读者不存在");
        }
        BookCopy copy = resolveCopy(request);
        return doBorrow(reader, copy);
    }

    private LoanResponse doBorrow(Reader reader, BookCopy copy) {
        // 读者状态检查：受限（停借）不可借阅
        if (reader.getStatus() != ReaderStatus.NORMAL) {
            throw new BusinessException(ErrorCodes.READER_RESTRICTED, "读者当前处于停借状态，不可借阅");
        }

        // 有未缴罚金不可借阅
        if (penaltyMapper.selectCount(Wrappers.<Penalty>lambdaQuery()
                .eq(Penalty::getReaderId, reader.getId())
                .eq(Penalty::getStatus, PenaltyStatus.UNPAID)) > 0) {
            throw new BusinessException(ErrorCodes.READER_RESTRICTED, "读者有未缴罚金，请先缴纳后再借阅");
        }

        // 借阅数量限制：学生 5 本，教师 10 本
        long activeCount = loanMapper.selectCount(Wrappers.<Loan>lambdaQuery()
                .eq(Loan::getReaderId, reader.getId())
                .in(Loan::getStatus, List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE)));
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
        // 书目状态需要单独查一次（实体间不再有关联导航）
        Book book = bookMapper.selectById(copy.getBookId());
        if (book != null && book.getStatus() == BookStatus.INACTIVE) {
            throw new BusinessException(ErrorCodes.COPY_NOT_AVAILABLE, "该图书已下架，不可借阅");
        }

        Loan loan = new Loan();
        loan.setCopyId(copy.getId());
        loan.setReaderId(reader.getId());
        loan.setBorrowedAt(LocalDateTime.now());
        loan.setDueDate(LocalDate.now().plusDays(policy.loanDays(reader.getType())));
        loan.setRenewedCount(0);
        loan.setStatus(LoanStatus.ACTIVE);
        loanMapper.insert(loan);

        // 把副本置为已借出；带 @Version 的乐观锁会拼上 where version = ?，
        // 更新行数为 0 说明副本已被并发借走，等价于原来 JPA 抛出的乐观锁异常。
        copy.setStatus(CopyStatus.BORROWED);
        if (copyMapper.updateById(copy) == 0) {
            throw new ConflictException("该副本刚被其他借阅占用，请刷新后重试");
        }

        return loanMapper.selectDetailById(loan.getId());
    }

    @Override
    @Transactional
    public LoanResponse returnLoan(Long loanId) {
        Loan loan = loanMapper.selectById(loanId);
        if (loan == null) {
            throw new NotFoundException("借阅记录不存在");
        }
        return doReturn(loan);
    }

    @Override
    @Transactional
    public LoanResponse returnSelf(AppPrincipal caller, Long loanId) {
        Loan loan = loanMapper.selectById(loanId);
        if (loan == null) {
            throw new NotFoundException("借阅记录不存在");
        }

        // 读者只能归还自己借阅的图书，管理员不受此限
        if (caller.role() != UserRole.ADMIN && !loan.getReaderId().equals(caller.userId())) {
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

        // 副本回到在架
        BookCopy copy = copyMapper.selectById(loan.getCopyId());
        if (copy != null) {
            copy.setStatus(CopyStatus.IN_STOCK);
            copyMapper.updateById(copy);
        }

        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnedAt(LocalDateTime.now());
        loanMapper.updateById(loan);

        return loanMapper.selectDetailById(loan.getId());
    }

    @Override
    @Transactional
    public LoanResponse renewLoan(Long loanId, AppPrincipal caller) {
        Loan loan = loanMapper.selectById(loanId);
        if (loan == null) {
            throw new NotFoundException("借阅记录不存在");
        }

        // 读者只能续借自己的图书
        if (caller.role() != UserRole.ADMIN && !loan.getReaderId().equals(caller.userId())) {
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

        Reader reader = readerMapper.selectById(loan.getReaderId());

        loan.setRenewedCount(loan.getRenewedCount() + 1);
        loan.setDueDate(loan.getDueDate().plusDays(policy.loanDays(reader.getType())));
        loanMapper.updateById(loan);

        return loanMapper.selectDetailById(loan.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<LoanResponse> getLoans(Long readerId, LoanStatus status, int page, int size) {
        IPage<LoanResponse> result = loanMapper.selectDetailPage(Pages.of(page, size), readerId, status);
        return PageResult.of(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<LoanResponse> getMyLoans(AppPrincipal caller, int page, int size) {
        IPage<LoanResponse> result = loanMapper.selectDetailPage(Pages.of(page, size), caller.userId(), null);
        return PageResult.of(result);
    }

    private Reader resolveReader(BorrowRequest request) {
        if (request.getReaderId() != null) {
            Reader reader = readerMapper.selectById(request.getReaderId());
            if (reader == null) {
                throw new NotFoundException("读者不存在");
            }
            return reader;
        }
        if (request.getReaderAccount() != null && !request.getReaderAccount().isBlank()) {
            Reader reader = readerMapper.selectOne(Wrappers.<Reader>lambdaQuery()
                    .eq(Reader::getAccount, request.getReaderAccount()));
            if (reader == null) {
                throw new NotFoundException("读者不存在");
            }
            return reader;
        }
        throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "请指定读者（readerId 或 readerAccount）");
    }

    private BookCopy resolveCopy(BorrowRequest request) {
        if (request.getCopyId() != null) {
            BookCopy copy = copyMapper.selectById(request.getCopyId());
            if (copy == null) {
                throw new NotFoundException("副本不存在");
            }
            return copy;
        }
        if (request.getBarcode() != null && !request.getBarcode().isBlank()) {
            BookCopy copy = copyMapper.selectOne(Wrappers.<BookCopy>lambdaQuery()
                    .eq(BookCopy::getBarcode, request.getBarcode()));
            if (copy == null) {
                throw new NotFoundException("副本不存在");
            }
            return copy;
        }
        throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "请指定副本（copyId 或 barcode）");
    }
}
