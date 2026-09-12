package com.school.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 在线咨询的对话消息（帮助与反馈「实时对话」，对应表 chat_message）。
 *
 * <p>会话按 readerId 归属：读者发给管理员的消息与管理员的回复都挂在读者的
 * readerId 下；「实时」由前端轮询实现（轮询只拉自己的会话，见 HelpController）。
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    /** 发送方角色：读者 */
    public static final String ROLE_READER = "READER";

    /** 发送方角色：管理员 */
    public static final String ROLE_ADMIN = "ADMIN";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 会话归属的读者 ID（管理员回复的消息也记在这上面） */
    private Long readerId;

    /** 发送方角色：READER 读者 / ADMIN 管理员 */
    private String senderRole;

    /** 发送方展示名 */
    private String senderName;

    /** 消息内容 */
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();
}
