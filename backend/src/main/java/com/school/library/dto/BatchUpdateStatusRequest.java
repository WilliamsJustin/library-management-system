package com.school.library.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 批量设置读者状态请求体（status：true 正常 / false 停借） */
public record BatchUpdateStatusRequest(
        @NotEmpty(message = "请选择要操作的读者")
        List<Long> ids,

        @NotNull(message = "请指定目标状态")
        Boolean status
) {
}
