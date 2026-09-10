package com.school.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 书目（对应表 book） */
@Data
@TableName("book")
public class Book {

    /** 主键，数据库自增（等价于原 JPA 的 GenerationType.IDENTITY） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank
    private String isbn;

    @NotBlank
    private String title;

    private String author;

    private String publisher;

    private String category;

    private BookStatus status = BookStatus.ACTIVE;
}
