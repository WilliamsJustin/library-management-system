package com.school.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/** 借阅记录（对应表 loan） */
@Data
@TableName("loan")
public class Loan {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 副本 ID —— 取代原 JPA 的 {@code @ManyToOne BookCopy copy} */
    @TableField("copy_id")
    private Long copyId;

    /** 读者 ID —— 取代原 JPA 的 {@code @ManyToOne Reader reader} */
    @TableField("reader_id")
    private Long readerId;

    private LocalDateTime borrowedAt;

    /**
     * 应还时间。借期单位为「分钟」（学生/教师均 10 分钟），所以这里必须精确到时刻，
     * 对应的库表列也由 DATE 改成了 DATETIME（见 db/upgrade-loan-minutes.sql）。
     */
    private LocalDateTime dueDate;

    private LocalDateTime returnedAt;

    private int renewedCount = 0;

    /** 到期前提醒是否已下发，保证同一笔借阅「只提醒一次」 */
    private boolean dueReminderSent = false;

    private LoanStatus status = LoanStatus.ACTIVE;

    /** 乐观锁版本（等价于原 JPA 的 @Version） */
    @Version
    private Long version;
}
