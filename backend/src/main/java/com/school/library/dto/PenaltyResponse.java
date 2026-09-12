package com.school.library.dto;

import com.school.library.entity.PenaltyStatus;
import com.school.library.entity.ReaderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "罚款账单")
public class PenaltyResponse {
    @Schema(description = "罚款ID")
    private Long id;

    @Schema(description = "关联借阅ID")
    private Long loanId;

    @Schema(description = "ISBN")
    private String isbn;

    @Schema(description = "书名")
    private String bookTitle;

    @Schema(description = "副本条形码")
    private String barcode;

    @Schema(description = "读者ID")
    private Long readerId;

    @Schema(description = "读者姓名")
    private String readerName;

    @Schema(description = "读者账号")
    private String readerAccount;

    @Schema(description = "读者学号/工号")
    private String readerNo;

    @Schema(description = "读者类型：STUDENT 学生 / TEACHER 教师")
    private ReaderType readerType;

    @Schema(description = "罚款金额（元）")
    private BigDecimal amount;

    @Schema(description = "状态：UNPAID 未缴 / PAID 已缴")
    private PenaltyStatus status;

    @Schema(description = "产生时间")
    private LocalDateTime createdAt;

    @Schema(description = "缴费时间")
    private LocalDateTime paidAt;

    // 说明：改用 MyBatis-Plus 后 Penalty 实体只持有 loanId / readerId，无法再由实体推导
    // 书名、条形码、读者姓名，因此原来的 fromEntity(Penalty) 已删除；
    // 本对象统一由 PenaltyMapper 的 join 查询直接映射。
}
