package com.school.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 罚款账单（对应表 penalty；loan_id 唯一约束保证定时任务与归还兜底的幂等） */
@Data
@TableName("penalty")
public class Penalty {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 产生罚金的借阅记录 ID —— 取代原 JPA 的 {@code @ManyToOne Loan loan} */
    @TableField("loan_id")
    private Long loanId;

    /** 读者 ID —— 取代原 JPA 的 {@code @ManyToOne Reader reader} */
    @TableField("reader_id")
    private Long readerId;

    private BigDecimal amount;

    private PenaltyStatus status = PenaltyStatus.UNPAID;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime paidAt;
}
