package com.school.library.dto;

import com.school.library.entity.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "站内消息")
public class NotificationResponse {
    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "类型：NORMAL 普通 / REMINDER 逾期到期提醒")
    private String type;

    @Schema(description = "时间")
    private LocalDateTime createdAt;

    @Schema(description = "是否已读")
    private Boolean read;

    public static NotificationResponse fromEntity(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setContent(notification.getContent());
        response.setType(notification.getType());
        response.setCreatedAt(notification.getCreatedAt());
        response.setRead(notification.isRead());
        return response;
    }
}
