package com.school.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    /** 出版日期 */
    private LocalDate publishDate;

    /** 语言 */
    private String language;

    /** 定价（元） */
    private BigDecimal price;

    /** 封面图片地址（外链或站内相对路径），可为空 */
    private String coverUrl;

    /** 内容简介 */
    private String description;

    private BookStatus status = BookStatus.ACTIVE;
}
