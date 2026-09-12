package com.school.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 管理员回复留言的请求体 */
public record ReplyFeedbackRequest(
        @NotBlank(message = "回复内容不能为空")
        @Size(max = 500, message = "回复最多 500 字")
        String content
) {
}
