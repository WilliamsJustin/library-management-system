package com.school.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建图书副本请求")
public class CreateBookCopyRequest {

    @Schema(description = "条形码")
    @NotBlank(message = "条形码不能为空")
    private String barcode;

    @Schema(description = "位置")
    @NotBlank(message = "位置不能为空")
    private String location;
}