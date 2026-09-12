package com.school.library.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.library.dto.DashboardStatsResponse;
import com.school.library.dto.DashboardStatsResponse.ChatOverview;
import com.school.library.dto.DashboardStatsResponse.LoanOverview;
import com.school.library.dto.DashboardStatsResponse.PenaltyOverview;
import com.school.library.entity.Loan;
import com.school.library.entity.LoanStatus;
import com.school.library.entity.Penalty;
import com.school.library.mapper.LoanMapper;
import com.school.library.mapper.PenaltyMapper;
import com.school.library.service.HelpService;
import com.school.library.service.StatsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

@Service
public class StatsServiceImpl implements StatsService {

    private final LoanMapper loanMapper;
    private final PenaltyMapper penaltyMapper;
    private final HelpService helpService;

    public StatsServiceImpl(LoanMapper loanMapper, PenaltyMapper penaltyMapper, HelpService helpService) {
        this.loanMapper = loanMapper;
        this.penaltyMapper = penaltyMapper;
        this.helpService = helpService;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse overview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        // ---- 借阅流通：按借出时间窗口统计借阅数；逾期数 = 当前处于逾期状态、
        //      且应还时间落在对应窗口内的记录（即该窗口内「变成逾期」的量）----
        long loansToday = loanMapper.selectCount(Wrappers.<Loan>lambdaQuery()
                .ge(Loan::getBorrowedAt, todayStart));
        long loansWeek = loanMapper.selectCount(Wrappers.<Loan>lambdaQuery()
                .ge(Loan::getBorrowedAt, weekStart));
        long loansTotal = loanMapper.selectCount(null);

        long overdueToday = loanMapper.selectCount(Wrappers.<Loan>lambdaQuery()
                .eq(Loan::getStatus, LoanStatus.OVERDUE)
                .ge(Loan::getDueDate, todayStart)
                .lt(Loan::getDueDate, now));
        long overdueWeek = loanMapper.selectCount(Wrappers.<Loan>lambdaQuery()
                .eq(Loan::getStatus, LoanStatus.OVERDUE)
                .ge(Loan::getDueDate, weekStart)
                .lt(Loan::getDueDate, now));
        long overdueTotal = loanMapper.selectCount(Wrappers.<Loan>lambdaQuery()
                .eq(Loan::getStatus, LoanStatus.OVERDUE));

        LoanOverview loans = new LoanOverview(loansToday, loansWeek, loansTotal,
                overdueToday, overdueWeek, overdueTotal);

        // ---- 罚款：数据量小，直接取全表在内存里按生成时间窗口求和（含已缴与未缴） ----
        var penalties = penaltyMapper.selectList(null);
        BigDecimal todayAmount = penalties.stream()
                .filter(p -> p.getCreatedAt() != null && !p.getCreatedAt().isBefore(todayStart))
                .map(Penalty::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = penalties.stream()
                .map(Penalty::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PenaltyOverview penalty = new PenaltyOverview(todayAmount, totalAmount);

        // ---- 实时咨询：口径与「帮助与反馈 → 实时对话」一致，均按未回复消息条数统计 ----
        ChatOverview chat = new ChatOverview(
                helpService.chatSessions().stream()
                        .mapToLong(com.school.library.dto.ChatSessionResponse::unrepliedCount)
                        .sum(),
                countUnpaidFeedback());

        return new DashboardStatsResponse(loans, penalty, chat);
    }

    private long countUnpaidFeedback() {
        return helpService.listFeedback("UNREPLIED", null, null, null, 0, 1).getTotalElements();
    }
}
