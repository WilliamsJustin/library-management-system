package com.school.library.dto;

import com.school.library.entity.Loan;
import com.school.library.entity.LoanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
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

    @Schema(description = "借出时间")
    private LocalDateTime borrowedAt;

    @Schema(description = "应还日期")
    private LocalDate dueDate;

    @Schema(description = "归还时间")
    private LocalDateTime returnedAt;

    @Schema(description = "已续借次数")
    private Integer renewedCount;

    @Schema(description = "状态：ACTIVE 在借 / RETURNED 已还 / OVERDUE 逾期")
    private LoanStatus status;

    public static LoanResponse fromEntity(Loan loan) {
        LoanResponse response = new LoanResponse();
        response.setId(loan.getId());
        response.setBookId(loan.getCopy().getBook().getId());
        response.setBookTitle(loan.getCopy().getBook().getTitle());
        response.setIsbn(loan.getCopy().getBook().getIsbn());
        response.setBarcode(loan.getCopy().getBarcode());
        response.setReaderId(loan.getReader().getId());
        response.setReaderName(loan.getReader().getName());
        response.setReaderAccount(loan.getReader().getAccount());
        response.setBorrowedAt(loan.getBorrowedAt());
        response.setDueDate(loan.getDueDate());
        response.setReturnedAt(loan.getReturnedAt());
        response.setRenewedCount(loan.getRenewedCount());
        response.setStatus(loan.getStatus());
        return response;
    }
}
