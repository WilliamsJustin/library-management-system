package com.school.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 站内消息（逾期提醒、即将逾期提醒等，对应表 notification；消息按 reader_id 私有隔离） */
@Data
@TableName("notification")
public class Notification {

    /** 普通站内消息（逾期结算、罚款、恢复资格等通知） */
    public static final String TYPE_NORMAL = "NORMAL";

    /** 逾期/到期提醒（读者端弹窗只弹这一类） */
    public static final String TYPE_REMINDER = "REMINDER";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("reader_id")
    private Long readerId;

    private String content;

    /** 消息类型：NORMAL 普通 / REMINDER 逾期到期提醒 */
    private String type = TYPE_NORMAL;

    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 是否已读。列名是 is_read，属性名是 read，
     * 驼峰下划线自动映射推不出这一层，必须显式指定。
     */
    @TableField("is_read")
    private boolean read = false;

    public Notification() {
    }

    public Notification(Long readerId, String content) {
        this(readerId, content, TYPE_NORMAL);
    }

    public Notification(Long readerId, String content, String type) {
        this.readerId = readerId;
        this.content = content;
        this.type = type;
    }
}
