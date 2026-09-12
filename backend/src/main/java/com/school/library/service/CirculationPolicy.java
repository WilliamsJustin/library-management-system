package com.school.library.service;

import com.school.library.entity.ReaderType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 借阅规则引擎（集中配置），数值全部来自 application.yml 的 {@code app.library.*}：
 * <ul>
 *   <li>借阅数量：学生最多 5 本，教师最多 10 本</li>
 *   <li>借期：学生与教师一致，均为 10 <b>分钟</b>（单位已由「天」改为「分钟」）</li>
 *   <li>续借：每本图书最多 1 次，续借期与原借期相同（即再顺延 10 分钟）</li>
 *   <li>逾期罚款：0.10 元/<b>分钟</b> × 逾期分钟数，单本累计封顶 144 元（1440 分钟封顶）</li>
 *   <li>到期提醒：到期前 5 分钟下发一次站内消息</li>
 * </ul>
 */
@Component
public class CirculationPolicy {

    private final int loanMinutes;
    private final BigDecimal finePerMinute;
    private final int reminderMinutes;
    private final long checkIntervalMs;
    private final int maxFineMinutes;

    public CirculationPolicy(@Value("${app.library.loan-minutes:10}") int loanMinutes,
                             @Value("${app.library.fine-per-minute:0.10}") BigDecimal finePerMinute,
                             @Value("${app.library.reminder-minutes:5}") int reminderMinutes,
                             @Value("${app.library.check-interval-ms:30000}") long checkIntervalMs,
                             @Value("${app.library.max-fine-minutes:1440}") int maxFineMinutes) {
        this.loanMinutes = loanMinutes;
        this.finePerMinute = finePerMinute;
        this.reminderMinutes = reminderMinutes;
        this.checkIntervalMs = checkIntervalMs;
        this.maxFineMinutes = maxFineMinutes;
    }

    /** 读者最大在借数量 */
    public int maxBorrowCount(ReaderType type) {
        return type == ReaderType.TEACHER ? 10 : 5;
    }

    /** 借期（分钟）。学生与教师一致，统一取配置值。 */
    public int loanMinutes(ReaderType type) {
        return loanMinutes;
    }

    /** 最大续借次数 */
    public int maxRenewCount() {
        return 1;
    }

    /** 逾期罚款单价（元/分钟） */
    public BigDecimal finePerMinute() {
        return finePerMinute;
    }

    /** 单本图书累计罚金上限对应的逾期分钟数（1440 分钟 = 一天封顶） */
    public int maxFineMinutes() {
        return maxFineMinutes;
    }

    /** 单本图书累计罚金上限（元） = 单价 × 封顶分钟数 */
    public BigDecimal maxFineAmount() {
        return finePerMinute.multiply(BigDecimal.valueOf(maxFineMinutes)).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** 到期前提醒阈值（分钟） */
    public int reminderMinutes() {
        return reminderMinutes;
    }

    /** 逾期结算与到期提醒的轮询间隔（毫秒） */
    public long checkIntervalMs() {
        return checkIntervalMs;
    }
}
