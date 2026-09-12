package com.school.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 提交留言的请求体（登录读者与游客均可） */
public record CreateFeedbackRequest(
        @NotBlank(message = "留言内容不能为空")
        @Size(max = 500, message = "留言最多 500 字")
        String content,

        /** 游客留言时的昵称 / 联系方式；登录读者可空（自动带账号名） */
        @Size(max = 50, message = "昵称最多 50 字")
        String contactName
) {
}
