package com.school.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.library.dto.NotificationResponse;
import com.school.library.entity.Notification;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 站内消息 Mapper */
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 某读者最近 50 条消息。
     * 实体属性名是 read、数据库列名是 is_read，自动映射推不出这一层，
     * 所以这里用 @Results 显式指定列到属性的映射。
     */
    @Select("""
            SELECT id, content, type, created_at, is_read
            FROM notification
            WHERE reader_id = #{readerId}
            ORDER BY created_at DESC, id DESC
            LIMIT 50
            """)
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "content", property = "content"),
            @Result(column = "type", property = "type"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "is_read", property = "read")
    })
    List<NotificationResponse> selectRecentByReader(@Param("readerId") Long readerId);

    /** 未读消息数（TRUE/FALSE 写法在 MySQL 与 H2 上都成立） */
    @Select("SELECT COUNT(*) FROM notification WHERE reader_id = #{readerId} AND is_read = FALSE")
    long countUnread(@Param("readerId") Long readerId);

    /** 全部标记已读，返回受影响行数 */
    @Update("UPDATE notification SET is_read = TRUE WHERE reader_id = #{readerId} AND is_read = FALSE")
    int markAllRead(@Param("readerId") Long readerId);
}
