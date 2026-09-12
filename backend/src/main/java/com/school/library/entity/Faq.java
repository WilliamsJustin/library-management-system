package com.school.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** FAQ 常见问题（帮助与反馈知识库，前台悬浮窗与读者后台共用，对应表 faq） */
@Data
@TableName("faq")
public class Faq {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 问题（关键词匹配的主要依据） */
    private String question;

    /** 答案 */
    private String answer;

    /** 是否启用（停用后前台检索不到） */
    private boolean enabled = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
