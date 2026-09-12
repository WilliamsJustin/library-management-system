package com.school.library.service;

import com.school.library.common.PageResult;
import com.school.library.dto.PenaltyResponse;
import com.school.library.entity.Loan;
import com.school.library.entity.PenaltyStatus;
import com.school.library.entity.ReaderType;
import com.school.library.security.AppPrincipal;

import java.time.LocalDate;

public interface PenaltyService {

    /**
     * 罚款查询（管理员），page 为 0 基。
     * keyword 模糊匹配账号/学号/姓名/ISBN/书名，readerType 可为 null 表示不限。
     */
    PageResult<PenaltyResponse> getPenalties(PenaltyStatus status, ReaderType readerType,
                                             String keyword, int page, int size);

    /**
     * 我的罚款（读者本人），page 为 0 基。
     * keyword 模糊匹配 ISBN/书名/条形码，status 可为 null 表示不限制。
     * 时间过滤：dateField 指定按生成时间（CREATED，默认）/ 缴费时间（PAID）哪一列，
     * startDate/endDate 为日期闭区间，均可为空。
     */
    PageResult<PenaltyResponse> getMyPenalties(AppPrincipal caller, PenaltyStatus status, String keyword,
                                               String dateField, LocalDate startDate, LocalDate endDate,
                                               int page, int size);

    /** 缴纳罚款（管理员代缴或读者自缴），缴清后自动恢复借阅资格 */
    void payPenalty(Long id, AppPrincipal caller);

    /**
     * 逾期结算（幂等）：为逾期借阅生成罚款、限制读者借阅并发送提醒。
     * 由定时任务与归还接口共同调用。
     */
    void settleOverdue(Loan loan);
}
