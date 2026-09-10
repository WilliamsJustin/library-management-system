package com.school.library.dto;

import com.school.library.entity.BookCopy;
import com.school.library.entity.CopyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "图书副本信息")
public class BookCopyResponse {
    @Schema(description = "副本ID")
    private Long id;

    @Schema(description = "所属图书ID")
    private Long bookId;

    @Schema(description = "条形码")
    private String barcode;

    @Schema(description = "位置")
    private String location;

    @Schema(description = "状态")
    private CopyStatus status;

    public static BookCopyResponse fromEntity(BookCopy copy) {
        BookCopyResponse response = new BookCopyResponse();
        response.setId(copy.getId());
        response.setBookId(copy.getBookId());
        response.setBarcode(copy.getBarcode());
        response.setLocation(copy.getLocation());
        response.setStatus(copy.getStatus());
        return response;
    }
}
