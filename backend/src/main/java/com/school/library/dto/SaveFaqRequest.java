package com.school.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 新增 / 编辑 FAQ 的请求体 */
public record SaveFaqRequest(
        @NotBlank(message = "问题不能为空")
        @Size(max = 200, message = "问题最多 200 字")
        String question,

        @NotBlank(message = "答案不能为空")
        @Size(max = 1000, message = "答案最多 1000 字")
        String answer,

        /** 不传默认启用 */
        Boolean enabled
) {
}
