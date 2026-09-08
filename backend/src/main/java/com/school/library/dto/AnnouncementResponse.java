package com.school.library.dto;

import com.school.library.entity.Announcement;

import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id,
        String title,
        String content,
        boolean pinned,
        LocalDateTime publishedAt
) {
    public static AnnouncementResponse fromEntity(Announcement a) {
        return new AnnouncementResponse(
                a.getId(),
                a.getTitle(),
                a.getContent(),
                a.isPinned(),
                a.getPublishedAt()
        );
    }
}
