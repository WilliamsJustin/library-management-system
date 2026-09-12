package com.school.library.dto;

import com.school.library.entity.Faq;

import java.time.LocalDateTime;

public record FaqResponse(
        Long id,
        String question,
        String answer,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FaqResponse fromEntity(Faq f) {
        return new FaqResponse(f.getId(), f.getQuestion(), f.getAnswer(),
                f.isEnabled(), f.getCreatedAt(), f.getUpdatedAt());
    }
}
