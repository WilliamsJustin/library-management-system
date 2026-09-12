package com.school.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** 管理员首页仪表盘统计数据 */
@Schema(description = "管理员首页统计数据")
public record DashboardStatsResponse(
        @Schema(description = "借阅流通概况")
        LoanOverview loans,

        @Schema(description = "逾期罚款概况")
        PenaltyOverview penalty,

        @Schema(description = "实时咨询概况")
        ChatOverview chat
) {
    @Schema(description = "借阅流通：按借出时间/应还时间窗口统计")
    public record LoanOverview(
            @Schema(description = "当日借阅数")
            long today,
            @Schema(description = "本周借阅数")
            long week,
            @Schema(description = "历史借阅数")
            long total,
            @Schema(description = "当日新增逾期数（应还时间落在今天的在途逾期）")
            long overdueToday,
            @Schema(description = "本周新增逾期数")
            long overdueWeek,
            @Schema(description = "历史逾期数（当前处于逾期状态的总量）")
            long overdueTotal
    ) {
    }

    @Schema(description = "罚款：按罚款生成时间统计的金额")
    public record PenaltyOverview(
            @Schema(description = "当日罚款金额（元）")
            BigDecimal todayAmount,
            @Schema(description = "历史罚款金额（元，含已缴与未缴）")
            BigDecimal totalAmount
    ) {
    }

    @Schema(description = "实时咨询：待处理消息数")
    public record ChatOverview(
            @Schema(description = "实时对话待回复会话数（最后一条是读者发的）")
            long chatPending,
            @Schema(description = "留言未回复数")
            long feedbackPending
    ) {
    }
}
