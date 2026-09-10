package com.school.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 图书馆公共公告（首页展示，对所有访客可见，对应表 announcement） */
@Data
@TableName("announcement")
public class Announcement {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** 置顶公告优先展示 */
    private boolean pinned = false;

    private LocalDateTime publishedAt = LocalDateTime.now();

    public Announcement() {
    }

    public Announcement(String title, String content, boolean pinned) {
        this.title = title;
        this.content = content;
        this.pinned = pinned;
    }
}
