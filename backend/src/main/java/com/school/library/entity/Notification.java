package com.school.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 站内消息（逾期提醒、即将逾期提醒等，对应表 notification） */
@Data
@TableName("notification")
public class Notification {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("reader_id")
    private Long readerId;

    private String content;

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
        this.readerId = readerId;
        this.content = content;
    }
}
