package com.school.library.dto;

import com.school.library.entity.LoanStatus;
import com.school.library.entity.ReaderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "借阅记录")
public class LoanResponse {
    @Schema(description = "借阅ID")
    private Long id;

    @Schema(description = "图书ID")
    private Long bookId;

    @Schema(description = "书名")
    private String bookTitle;

    @Schema(description = "ISBN")
    private String isbn;

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

    @Schema(description = "借出时间")
    private LocalDateTime borrowedAt;

    @Schema(description = "应还时间（精确到分钟，借期为 10 分钟）")
    private LocalDateTime dueDate;

    @Schema(description = "归还时间")
    private LocalDateTime returnedAt;

    @Schema(description = "已续借次数")
    private Integer renewedCount;

    @Schema(description = "状态：ACTIVE 在借 / RETURNED 已还 / OVERDUE 逾期")
    private LoanStatus status;

    // 说明：改用 MyBatis-Plus 后 Loan 实体只持有 copyId / readerId，无法再由实体自身
    // 推导出书名、条形码、读者姓名/账号/学号/类型，因此原来的 fromEntity(Loan) 已删除；
    // 本对象统一由 LoanMapper 的 join 查询（selectDetailById / selectDetailPage）直接映射。
}
