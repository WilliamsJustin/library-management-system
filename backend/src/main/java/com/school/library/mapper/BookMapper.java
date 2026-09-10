package com.school.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.library.entity.Book;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 书目 Mapper。
 * 单表增删改查由 {@link BaseMapper} 提供，动态条件查询在 Service 层用 LambdaQueryWrapper 拼装，
 * 这里只放 Wrapper 表达不了的聚合/去重查询。
 */
public interface BookMapper extends BaseMapper<Book> {

    /** 全部图书分类（去重排序），供前端下拉筛选 */
    @Select("""
            SELECT DISTINCT category
            FROM book
            WHERE category IS NOT NULL AND category <> ''
            ORDER BY category
            """)
    List<String> selectDistinctCategories();

    /** 全部出版社（去重排序），供前端下拉筛选 */
    @Select("""
            SELECT DISTINCT publisher
            FROM book
            WHERE publisher IS NOT NULL AND publisher <> ''
            ORDER BY publisher
            """)
    List<String> selectDistinctPublishers();
}
