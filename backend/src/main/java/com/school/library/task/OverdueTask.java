package com.school.library.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.library.entity.Loan;
import com.school.library.entity.LoanStatus;
import com.school.library.entity.Notification;
import com.school.library.mapper.LoanMapper;
import com.school.library.mapper.NotificationMapper;
import com.school.library.service.CirculationPolicy;
import com.school.library.service.PenaltyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 逾期结算 + 到期提醒。
 *
 * <p>借期单位已由「天」改为「分钟」（学生/教师均为 10 分钟），所以检查频率也从原来的
 * 「每天 01:00 跑一次」提升为高频轮询（默认每 30 秒，见 {@code app.library.check-interval-ms}）：
 * <ol>
 *   <li><b>逾期结算</b>：把已过应还时间且仍在借的记录标记为逾期、生成罚款、限制借阅并下发提醒（幂等）</li>
 *   <li><b>到期提醒</b>：应还时间前 {@code app.library.reminder-minutes}（默认 5 分钟）内下发一次站内消息，
 *       靠 {@code loan.due_reminder_sent} 保证「只提醒一次」</li>
 * </ol>
 *
 * <p>业务方法 {@link #checkOnce()} 与定时触发器分开：触发器是内部静态类，可用
 * {@code app.library.scheduling-enabled=false} 整体关闭——测试环境必须关掉，
 * 否则 30 秒一轮的定时器会跑进测试事务里干扰断言。
 */
@Component
public class OverdueTask {

    private static final Logger log = LogManager.getLogger(OverdueTask.class);

    private static final DateTimeFormatter MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private LoanMapper loanMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private PenaltyService penaltyService;

    @Autowired
    private CirculationPolicy policy;

    /** 执行一轮检查：先结算逾期，再发即将到期提醒。定时器与测试都调用它。 */
    @Transactional
    public void checkOnce() {
        LocalDateTime now = LocalDateTime.now();
        settleOverdueLoans(now);
        sendDueSoonReminders(now);
    }

    /** 1) 逾期结算：标记逾期 + 生成罚款（幂等）+ 限制借阅 + 提醒 */
    private void settleOverdueLoans(LocalDateTime now) {
        // 已逾期的在途借阅（ACTIVE + OVERDUE）每轮都要重新结算：
        // ACTIVE 的标记为逾期，OVERDUE 的刷新罚款金额（0.10 元/分钟 × 逾期分钟数持续增长）
        List<Loan> overdue = loanMapper.selectList(Wrappers.<Loan>lambdaQuery()
                .in(Loan::getStatus, List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE))
                .lt(Loan::getDueDate, now));
        for (Loan loan : overdue) {
            if (loan.getStatus() == LoanStatus.ACTIVE) {
                loan.setStatus(LoanStatus.OVERDUE);
                loanMapper.updateById(loan);
            }
            penaltyService.settleOverdue(loan);
        }
    }

    /** 2) 到期提醒：应还时间前 N 分钟内提醒一次（每笔借阅只发一次） */
    private void sendDueSoonReminders(LocalDateTime now) {
        LocalDateTime threshold = now.plusMinutes(policy.reminderMinutes());
        List<Loan> dueSoon = loanMapper.selectList(Wrappers.<Loan>lambdaQuery()
                .eq(Loan::getStatus, LoanStatus.ACTIVE)
                .eq(Loan::isDueReminderSent, false)
                .ge(Loan::getDueDate, now)
                .le(Loan::getDueDate, threshold));

        for (Loan loan : dueSoon) {
            long minutesLeft = Math.max(ChronoUnit.MINUTES.between(now, loan.getDueDate()), 1);
            String bookTitle = loanMapper.selectBookTitleByLoanId(loan.getId());
            // 逾期/到期提醒是读者私有消息（按 reader_id 隔离，只有本人可见），
            // 打上 REMINDER 类型标记后，读者端弹窗与消息中心都以此区分
            notificationMapper.insert(new Notification(loan.getReaderId(),
                    "您借阅的《" + bookTitle + "》将于 " + loan.getDueDate().format(MINUTE_FORMAT)
                            + " 到期（还剩 " + minutesLeft + " 分钟），请及时归还或续借。",
                    Notification.TYPE_REMINDER));

            // 标记已提醒，保证「只提醒一次」；若之后续借，续借时会把标记重置
            loan.setDueReminderSent(true);
            loanMapper.updateById(loan);

            log.info("已下发到期提醒：loanId={} readerId={} dueDate={} 剩余={}分钟",
                    loan.getId(), loan.getReaderId(), loan.getDueDate().format(MINUTE_FORMAT), minutesLeft);
        }
    }

    /**
     * 定时触发器。单独拆成内部静态 bean，方便用 {@code app.library.scheduling-enabled=false} 整体关掉
     * （测试环境用），业务逻辑仍在外部类里，不受影响。
     */
    @Component
    @ConditionalOnProperty(prefix = "app.library", name = "scheduling-enabled",
            havingValue = "true", matchIfMissing = true)
    public static class Scheduler {

        private final OverdueTask task;

        public Scheduler(OverdueTask task) {
            this.task = task;
        }

        @Scheduled(fixedDelayString = "${app.library.check-interval-ms:30000}")
        public void run() {
            task.checkOnce();
        }
    }
}
