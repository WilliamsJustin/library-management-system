package com.school.library.dto;

import java.time.LocalDateTime;

/** 管理员端「实时对话」的会话列表项（按读者聚合） */
public record ChatSessionResponse(
        Long readerId,
        String readerName,
        String lastMessage,
        String lastSenderRole,
        LocalDateTime lastTime,
        long messageCount,
        /** 未回复消息数：最近一条管理员回复之后的读者消息条数 */
        long unrepliedCount
) {
}
