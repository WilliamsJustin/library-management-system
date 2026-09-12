package com.school.library.dto;

import com.school.library.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long readerId,
        String senderRole,
        String senderName,
        String content,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse fromEntity(ChatMessage m) {
        return new ChatMessageResponse(m.getId(), m.getReaderId(), m.getSenderRole(),
                m.getSenderName(), m.getContent(), m.getCreatedAt());
    }
}
