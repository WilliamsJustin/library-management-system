package com.school.library.service;

import com.school.library.common.PageResult;
import com.school.library.dto.BorrowRequest;
import com.school.library.dto.LoanResponse;
import com.school.library.entity.LoanStatus;
import com.school.library.entity.ReaderType;
import com.school.library.security.AppPrincipal;

import java.time.LocalDate;

public interface LoanService {

    /** 借出（管理员代借，需指定读者） */
    LoanResponse borrow(BorrowRequest request);

    /** 读者自助借阅（借阅人为当前登录读者本人） */
    LoanResponse borrowSelf(AppPrincipal caller, BorrowRequest request);

    /** 归还（管理员代还，逾期归还会兜底生成罚款） */
    LoanResponse returnLoan(Long loanId);

    /** 读者自助还书（仅限归还本人借阅的图书，逾期归还会兜底生成罚款） */
    LoanResponse returnSelf(AppPrincipal caller, Long loanId);

    /** 续借（读者只能续借自己的图书，且最多 1 次） */
    LoanResponse renewLoan(Long loanId, AppPrincipal caller);

    /** 借阅查询（管理员），可按读者类型筛选，keyword 模糊匹配账号/学号/姓名/ISBN/书名，page 为 0 基 */
    PageResult<LoanResponse> getLoans(Long readerId, LoanStatus status, ReaderType readerType,
                                      String keyword, int page, int size);

    /**
     * 我的借阅（读者本人，含历史），page 为 0 基。
     * keyword 模糊匹配 ISBN/书名/条形码，status 可为 null 表示不限制。
     * 时间过滤：dateField 指定按借出时间（BORROWED，默认）/ 应还时间（DUE）/
     * 归还时间（RETURNED）哪一列，startDate/endDate 为日期闭区间，均可为空。
     */
    PageResult<LoanResponse> getMyLoans(AppPrincipal caller, LoanStatus status, String keyword,
                                        String dateField, LocalDate startDate, LocalDate endDate,
                                        int page, int size);
}
