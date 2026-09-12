package com.school.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 发送对话消息的请求体 */
public record SendChatRequest(
        /** 管理员回复时指定目标读者；读者发送时忽略此字段（固定发给自己的会话） */
        Long readerId,

        @NotBlank(message = "消息内容不能为空")
        @Size(max = 500, message = "消息最多 500 字")
        String content
) {
}
