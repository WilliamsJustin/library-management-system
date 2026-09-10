package com.school.library.dto;

import com.school.library.entity.Activity;

import java.time.LocalDateTime;

public record ActivityResponse(
        Long id,
        String title,
        String content,
        String dateText,
        String tag,
        boolean pinned,
        LocalDateTime createdAt
) {
    public static ActivityResponse fromEntity(Activity a) {
        return new ActivityResponse(
                a.getId(),
                a.getTitle(),
                a.getContent(),
                a.getDateText(),
                a.getTag(),
                a.isPinned(),
                a.getCreatedAt()
        );
    }
}
