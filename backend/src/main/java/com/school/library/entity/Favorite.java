package com.school.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 读者收藏的书目（对应表 favorite）。
 *
 * <p>同一读者对同一本书只会有一条记录，靠数据库唯一键 uk_favorite_reader_book 保证，
 * 业务层重复收藏按幂等处理（不报错、不重复插入）。
 */
@Data
@TableName("favorite")
public class Favorite {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("reader_id")
    private Long readerId;

    @TableField("book_id")
    private Long bookId;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Favorite() {
    }

    public Favorite(Long readerId, Long bookId) {
        this.readerId = readerId;
        this.bookId = bookId;
    }
}
