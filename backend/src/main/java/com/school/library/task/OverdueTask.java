package com.school.library.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.library.entity.Loan;
import com.school.library.entity.LoanStatus;
import com.school.library.entity.Notification;
import com.school.library.mapper.LoanMapper;
import com.school.library.mapper.NotificationMapper;
import com.school.library.service.CirculationPolicy;
import com.school.library.service.PenaltyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 逾期检查定时任务：每天 01:00 执行。
 * 1) 将到期未还的借阅标记为逾期，生成罚款、限制借阅并发送提醒（幂等）
 * 2) 对即将到期的借阅发送到期提醒
 */
@Component
public class OverdueTask {

    @Autowired
    private LoanMapper loanMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private PenaltyService penaltyService;

    @Autowired
    private CirculationPolicy policy;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void dailyCheck() {
        LocalDate today = LocalDate.now();

        // 1) 逾期结算：标记逾期 + 生成罚款（幂等）+ 限制借阅 + 提醒
        List<Loan> overdue = loanMapper.selectList(Wrappers.<Loan>lambdaQuery()
                .eq(Loan::getStatus, LoanStatus.ACTIVE)
                .lt(Loan::getDueDate, today));
        for (Loan loan : overdue) {
            loan.setStatus(LoanStatus.OVERDUE);
            loanMapper.updateById(loan);
            penaltyService.settleOverdue(loan);
        }

        // 2) 即将到期提醒：到期前 N 天提醒一次
        int reminderDays = policy.reminderDays();
        List<Loan> dueSoon = loanMapper.selectList(Wrappers.<Loan>lambdaQuery()
                .eq(Loan::getStatus, LoanStatus.ACTIVE)
                .between(Loan::getDueDate, today, today.plusDays(reminderDays)));
        for (Loan loan : dueSoon) {
            long daysLeft = ChronoUnit.DAYS.between(today, loan.getDueDate());
            if (daysLeft == reminderDays) {
                String bookTitle = loanMapper.selectBookTitleByLoanId(loan.getId());
                notificationMapper.insert(new Notification(loan.getReaderId(),
                        "您借阅的《" + bookTitle + "》将于 "
                                + loan.getDueDate() + " 到期（还剩 " + daysLeft + " 天），请及时归还或续借。"));
            }
        }
    }
}
