package com.school.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
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

    private LocalDate dueDate;

    private LocalDateTime returnedAt;

    private int renewedCount = 0;

    private LoanStatus status = LoanStatus.ACTIVE;

    /** 乐观锁版本（等价于原 JPA 的 @Version） */
    @Version
    private Long version;
}
