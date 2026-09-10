package com.school.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "发布公告请求")
public class CreateAnnouncementRequest {

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    @Size(max = 120, message = "标题不超过 120 字")
    private String title;

    @Schema(description = "内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "内容不能为空")
    @Size(max = 1000, message = "内容不超过 1000 字")
    private String content;

    @Schema(description = "是否置顶", defaultValue = "false")
    private boolean pinned = false;
}
