package com.school.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "借出请求：读者与副本均可通过 ID 或业务标识定位，二选一")
public class BorrowRequest {

    @Schema(description = "读者ID（与 readerAccount 二选一）")
    private Long readerId;

    @Schema(description = "读者账号（与 readerId 二选一）")
    private String readerAccount;

    @Schema(description = "副本ID（与 barcode 二选一）")
    private Long copyId;

    @Schema(description = "副本条形码（与 copyId 二选一）")
    private String barcode;
}
