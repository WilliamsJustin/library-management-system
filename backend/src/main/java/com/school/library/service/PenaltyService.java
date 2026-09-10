package com.school.library.service;

import com.school.library.common.PageResult;
import com.school.library.dto.PenaltyResponse;
import com.school.library.entity.Loan;
import com.school.library.entity.PenaltyStatus;
import com.school.library.security.AppPrincipal;

public interface PenaltyService {

    /** 罚款查询（管理员），page 为 0 基 */
    PageResult<PenaltyResponse> getPenalties(PenaltyStatus status, int page, int size);

    /** 我的罚款（读者本人），page 为 0 基 */
    PageResult<PenaltyResponse> getMyPenalties(AppPrincipal caller, int page, int size);

    /** 缴纳罚款（管理员代缴或读者自缴），缴清后自动恢复借阅资格 */
    void payPenalty(Long id, AppPrincipal caller);

    /**
     * 逾期结算（幂等）：为逾期借阅生成罚款、限制读者借阅并发送提醒。
     * 由定时任务与归还接口共同调用。
     */
    void settleOverdue(Loan loan);
}
