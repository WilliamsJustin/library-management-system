package com.school.library.dto;

import com.school.library.entity.BookStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 批量设置图书状态请求体（status：ACTIVE 上架 / INACTIVE 下架） */
public record BatchBookStatusRequest(
        @NotEmpty(message = "请选择要操作的图书")
        List<Long> ids,

        @NotNull(message = "请指定目标状态")
        BookStatus status
) {
}
