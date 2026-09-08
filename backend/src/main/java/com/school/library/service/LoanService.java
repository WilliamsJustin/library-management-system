package com.school.library.service;

import com.school.library.dto.BorrowRequest;
import com.school.library.dto.LoanResponse;
import com.school.library.entity.LoanStatus;
import com.school.library.security.AppPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LoanService {

    /** 借出 */
    LoanResponse borrow(BorrowRequest request);

    /** 归还（逾期归还会兜底生成罚款） */
    LoanResponse returnLoan(Long loanId);

    /** 续借（读者只能续借自己的图书，且最多 1 次） */
    LoanResponse renewLoan(Long loanId, AppPrincipal caller);

    /** 借阅查询（管理员） */
    Page<LoanResponse> getLoans(Long readerId, LoanStatus status, Pageable pageable);

    /** 我的借阅（读者本人，含历史） */
    Page<LoanResponse> getMyLoans(AppPrincipal caller, Pageable pageable);
}
