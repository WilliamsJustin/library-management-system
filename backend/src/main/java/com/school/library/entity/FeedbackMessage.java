package com.school.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 留言反馈（帮助与反馈的「留言板」，对应表 feedback_message）。
 *
 * <p>允许游客留言（readerId 为空），登录读者留言时自动带上账号信息；
 * 管理员回复后 status 置为 REPLIED 并记录回复人与时间。
 */
@Data
@TableName("feedback_message")
public class FeedbackMessage {

    /** 未回复 */
    public static final String STATUS_UNREPLIED = "UNREPLIED";

    /** 已回复 */
    public static final String STATUS_REPLIED = "REPLIED";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 留言读者；游客留言为 null */
    private Long readerId;

    /** 展示名：读者姓名 / 游客昵称 */
    private String readerName;

    /** 留言内容 */
    private String content;

    /** 管理员回复内容 */
    private String replyContent;

    /** 状态：UNREPLIED 未回复 / REPLIED 已回复 */
    private String status = STATUS_UNREPLIED;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime repliedAt;

    /** 回复的管理员账号 */
    private String repliedBy;
}
