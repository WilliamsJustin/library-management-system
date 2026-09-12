package com.school.library.dto;

import com.school.library.entity.FeedbackMessage;

import java.time.LocalDateTime;

public record FeedbackResponse(
        Long id,
        Long readerId,
        /** 留言人展示名：读者姓名 / 游客昵称 */
        String readerName,
        /** 读者账号；游客留言为 null */
        String readerAccount,
        /** 读者学号/工号；游客留言为 null */
        String readerNo,
        String content,
        String replyContent,
        String status,
        LocalDateTime createdAt,
        LocalDateTime repliedAt,
        String repliedBy
) {
    public static FeedbackResponse fromEntity(FeedbackMessage m) {
        // 实体只有 readerName（读者账号或游客昵称），账号/学号由 Mapper 的 join 查询带出
        return new FeedbackResponse(m.getId(), m.getReaderId(), m.getReaderName(),
                null, null, m.getContent(), m.getReplyContent(), m.getStatus(),
                m.getCreatedAt(), m.getRepliedAt(), m.getRepliedBy());
    }
}
