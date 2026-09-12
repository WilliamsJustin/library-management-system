package com.school.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.library.dto.FeedbackResponse;
import com.school.library.entity.FeedbackMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/** 留言 Mapper：列表用 LEFT JOIN reader 一次查全（读者可能已注销，留言仍要展示） */
public interface FeedbackMessageMapper extends BaseMapper<FeedbackMessage> {

    /** 留言明细的公共 SELECT 片段。读者行带出账号/学号；游客（reader_id 为空）展示名用留言时昵称 */
    String DETAIL_SELECT = """
            SELECT f.id            AS id,
                   f.reader_id     AS reader_id,
                   COALESCE(r.name, f.reader_name) AS reader_name,
                   r.account       AS reader_account,
                   r.student_no    AS reader_no,
                   f.content       AS content,
                   f.reply_content AS reply_content,
                   f.status        AS status,
                   f.created_at    AS created_at,
                   f.replied_at    AS replied_at,
                   f.replied_by    AS replied_by
            FROM feedback_message f
            LEFT JOIN reader r ON r.id = f.reader_id
            """;

    /**
     * 留言分页：readerId / status / keyword / 时间区间均可为空（不限制）。
     * keyword 模糊匹配账号 / 学号 / 留言人（读者姓名或游客昵称）；
     * 时间过滤固定按留言时间（f.created_at），start/endExclusive 为半开区间 [start, end)。
     */
    @Select("<script>"
            + DETAIL_SELECT
            + "<where>"
            + "  <if test='readerId != null'>AND f.reader_id = #{readerId}</if>"
            + "  <if test='status != null and status != \"\"'>AND f.status = #{status}</if>"
            + "  <if test='keyword != null and keyword != \"\"'>"
            + "    AND (r.account LIKE CONCAT('%', #{keyword}, '%')"
            + "      OR r.student_no LIKE CONCAT('%', #{keyword}, '%')"
            + "      OR COALESCE(r.name, f.reader_name) LIKE CONCAT('%', #{keyword}, '%'))"
            + "  </if>"
            + "  <if test='start != null'>AND f.created_at &gt;= #{start}</if>"
            + "  <if test='endExclusive != null'>AND f.created_at &lt; #{endExclusive}</if>"
            + "</where>"
            + " ORDER BY f.id DESC"
            + "</script>")
    IPage<FeedbackResponse> selectDetailPage(IPage<FeedbackResponse> page,
                                             @Param("readerId") Long readerId,
                                             @Param("status") String status,
                                             @Param("keyword") String keyword,
                                             @Param("start") LocalDateTime start,
                                             @Param("endExclusive") LocalDateTime endExclusive);
}
