package com.school.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.library.entity.Favorite;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 收藏 Mapper */
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 某读者的收藏分页（按收藏时间倒序，最新收藏在前）。
     * 自定义方法带 IPage 参数时，MyBatis-Plus 的分页插件会自动改写 SQL 并执行 count，
     * 不需要自己写 LIMIT。
     */
    @Select("""
            SELECT id, reader_id, book_id, created_at
            FROM favorite
            WHERE reader_id = #{readerId}
            ORDER BY created_at DESC, id DESC
            """)
    IPage<Favorite> selectPageByReader(IPage<Favorite> page, @Param("readerId") Long readerId);

    /** 某读者收藏的全部图书 ID（供前台标记「已收藏」状态，一次取完，不分页） */
    @Select("""
            SELECT book_id
            FROM favorite
            WHERE reader_id = #{readerId}
            ORDER BY created_at DESC, id DESC
            """)
    List<Long> selectBookIdsByReader(@Param("readerId") Long readerId);
}
