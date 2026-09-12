package com.school.library.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 批量重置密码请求体 */
public record BatchResetPasswordRequest(
        @NotEmpty(message = "请选择要操作的读者")
        List<Long> ids
) {
}
