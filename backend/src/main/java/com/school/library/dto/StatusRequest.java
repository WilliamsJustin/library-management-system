package com.school.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "状态更新请求")
public class StatusRequest {

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private Boolean status;
}