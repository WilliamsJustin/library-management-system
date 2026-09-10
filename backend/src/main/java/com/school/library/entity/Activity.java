package com.school.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 读者活动（前台"读者活动"栏目展示），对所有访客可见，对应表 activity */
@Data
@TableName("activity")
public class Activity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** 活动类别标签，如 校级/培训/沙龙/活动/竞赛 */
    private String tag;

    /** 置顶活动优先展示 */
    private boolean pinned = false;

    /** 发布时间（自动生成，精确到分钟） */
    private LocalDateTime createdAt = LocalDateTime.now();

    public Activity() {
    }

    public Activity(String title, String content, String tag, boolean pinned) {
        this.title = title;
        this.content = content;
        this.tag = tag;
        this.pinned = pinned;
    }
}
