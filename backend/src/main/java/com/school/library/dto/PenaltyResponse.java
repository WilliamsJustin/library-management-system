package com.school.library.dto;

import com.school.library.entity.Penalty;
import com.school.library.entity.PenaltyStatus;
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

    @Schema(description = "罚款金额（元）")
    private BigDecimal amount;

    @Schema(description = "状态：UNPAID 未缴 / PAID 已缴")
    private PenaltyStatus status;

    @Schema(description = "产生时间")
    private LocalDateTime createdAt;

    @Schema(description = "缴费时间")
    private LocalDateTime paidAt;

    public static PenaltyResponse fromEntity(Penalty penalty) {
        PenaltyResponse response = new PenaltyResponse();
        response.setId(penalty.getId());
        response.setLoanId(penalty.getLoan().getId());
        response.setBookTitle(penalty.getLoan().getCopy().getBook().getTitle());
        response.setBarcode(penalty.getLoan().getCopy().getBarcode());
        response.setReaderId(penalty.getReader().getId());
        response.setReaderName(penalty.getReader().getName());
        response.setReaderAccount(penalty.getReader().getAccount());
        response.setAmount(penalty.getAmount());
        response.setStatus(penalty.getStatus());
        response.setCreatedAt(penalty.getCreatedAt());
        response.setPaidAt(penalty.getPaidAt());
        return response;
    }
}
