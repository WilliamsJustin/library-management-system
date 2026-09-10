package com.school.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建/修改读者活动请求")
public class CreateActivityRequest {

    @Schema(description = "活动标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "活动标题不能为空")
    @Size(max = 120, message = "活动标题不超过 120 字")
    private String title;

    @Schema(description = "活动介绍", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "活动介绍不能为空")
    @Size(max = 1000, message = "活动介绍不超过 1000 字")
    private String content;

    @Schema(description = "时间文案，如 9月15日 14:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "时间不能为空")
    @Size(max = 60, message = "时间不超过 60 字")
    private String dateText;

    @Schema(description = "类别标签，如 校级/培训/沙龙", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "类别标签不能为空")
    @Size(max = 20, message = "类别标签不超过 20 字")
    private String tag;

    @Schema(description = "是否置顶", defaultValue = "false")
    private boolean pinned = false;
}
