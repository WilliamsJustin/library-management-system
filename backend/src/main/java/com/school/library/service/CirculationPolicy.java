package com.school.library.service;

import com.school.library.entity.ReaderType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 借阅规则引擎（集中配置）：
 * - 学生最多可借 5 本，借期 30 天；教师最多可借 10 本，借期 60 天
 * - 每本图书最多续借 1 次，续借期与原借期相同
 * - 逾期罚款按天累计，每日罚金与提醒提前天数见 application.yml
 */
@Component
public class CirculationPolicy {

    private final BigDecimal finePerDay;
    private final int reminderDays;

    public CirculationPolicy(@Value("${app.library.fine-per-day:0.10}") BigDecimal finePerDay,
                             @Value("${app.library.reminder-days:3}") int reminderDays) {
        this.finePerDay = finePerDay;
        this.reminderDays = reminderDays;
    }

    /** 读者最大在借数量 */
    public int maxBorrowCount(ReaderType type) {
        return type == ReaderType.TEACHER ? 10 : 5;
    }

    /** 借期（天） */
    public int loanDays(ReaderType type) {
        return type == ReaderType.TEACHER ? 60 : 30;
    }

    /** 最大续借次数 */
    public int maxRenewCount() {
        return 1;
    }

    /** 每日罚金（元） */
    public BigDecimal finePerDay() {
        return finePerDay;
    }

    /** 到期前提醒天数 */
    public int reminderDays() {
        return reminderDays;
    }
}
