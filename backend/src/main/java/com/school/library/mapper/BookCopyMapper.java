package com.school.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.library.dto.BookCopyStat;
import com.school.library.entity.BookCopy;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/** 图书副本 Mapper */
public interface BookCopyMapper extends BaseMapper<BookCopy> {

    /**
     * 按图书 ID 批量统计副本总数与在架数。
     * 图书列表每页只需要一次查询即可填满所有行，替代原来「每行两次 count」的写法。
     */
    @Select("""
            <script>
            SELECT book_id AS book_id,
                   COUNT(*) AS total_copies,
                   SUM(CASE WHEN status = 'IN_STOCK' THEN 1 ELSE 0 END) AS available_copies
            FROM book_copy
            WHERE book_id IN
            <foreach collection="bookIds" item="bookId" open="(" separator="," close=")">#{bookId}</foreach>
            GROUP BY book_id
            </script>
            """)
    List<BookCopyStat> selectStatsByBookIds(@Param("bookIds") Collection<Long> bookIds);
}
