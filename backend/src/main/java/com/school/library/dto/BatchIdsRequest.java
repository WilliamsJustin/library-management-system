package com.school.library.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 通用批量 ID 请求体（批量删除等场景） */
public record BatchIdsRequest(
        @NotEmpty(message = "请选择要操作的条目")
        List<Long> ids
) {
}
