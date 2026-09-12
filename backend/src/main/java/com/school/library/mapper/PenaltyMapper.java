package com.school.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.library.dto.PenaltyResponse;
import com.school.library.entity.Penalty;
import com.school.library.entity.PenaltyStatus;
import com.school.library.entity.ReaderType;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/** 罚款 Mapper：明细同样用 join 一次查全（罚单 -> 借阅 -> 副本 -> 书目 / 读者） */
public interface PenaltyMapper extends BaseMapper<Penalty> {

    /** 罚款明细的公共 SELECT 片段 */
    String DETAIL_SELECT = """
            SELECT p.id         AS id,
                   p.loan_id    AS loan_id,
                   p.amount     AS amount,
                   p.status     AS status,
                   p.created_at AS created_at,
                   p.paid_at    AS paid_at,
                   b.isbn       AS isbn,
                   b.title      AS book_title,
                   c.barcode    AS barcode,
                   r.id         AS reader_id,
                   r.name       AS reader_name,
                   r.account    AS reader_account,
                   r.student_no AS reader_no,
                   r.type       AS reader_type
            FROM penalty p
            JOIN loan l ON l.id = p.loan_id
            JOIN book_copy c ON c.id = l.copy_id
            JOIN book b ON b.id = c.book_id
            JOIN reader r ON r.id = p.reader_id
            """;

    @Select(DETAIL_SELECT + " WHERE p.id = #{id}")
    PenaltyResponse selectDetailById(@Param("id") Long id);

    /** 罚款查询分页：各条件均可为 null（表示不限制），keyword 模糊匹配账号/学号/姓名/ISBN/书名，按 ID 倒序 */
    @Select("<script>"
            + DETAIL_SELECT
            + "<where>"
            + "  <if test='readerId != null'>AND p.reader_id = #{readerId}</if>"
            + "  <if test='status != null'>AND p.status = #{status}</if>"
            + "  <if test='readerType != null'>AND r.type = #{readerType}</if>"
            + "  <if test='keyword != null and keyword != \"\"'>"
            + "    AND (r.account LIKE CONCAT('%', #{keyword}, '%')"
            + "      OR r.student_no LIKE CONCAT('%', #{keyword}, '%')"
            + "      OR r.name LIKE CONCAT('%', #{keyword}, '%')"
            + "      OR b.isbn LIKE CONCAT('%', #{keyword}, '%')"
            + "      OR b.title LIKE CONCAT('%', #{keyword}, '%'))"
            + "  </if>"
            + "</where>"
            + " ORDER BY p.id DESC"
            + "</script>")
    IPage<PenaltyResponse> selectDetailPage(IPage<PenaltyResponse> page,
                                            @Param("readerId") Long readerId,
                                            @Param("status") PenaltyStatus status,
                                            @Param("readerType") ReaderType readerType,
                                            @Param("keyword") String keyword);

    /**
     * 我的罚款分页：keyword 模糊匹配 ISBN/书名/条形码，status 可为 null（不限制），按 ID 倒序。
     * 时间过滤：dateField 指定按哪一列（CREATED 生成 / PAID 缴费，空默认生成），
     * 列名由 <choose> 的固定分支决定（不存在拼接注入），start/endExclusive 为半开区间 [start, end)。
     */
    @Select("<script>"
            + DETAIL_SELECT
            + "<where>"
            + "  p.reader_id = #{readerId}"
            + "  <if test='status != null'>AND p.status = #{status}</if>"
            + "  <if test='keyword != null and keyword != \"\"'>"
            + "    AND (b.isbn LIKE CONCAT('%', #{keyword}, '%')"
            + "      OR b.title LIKE CONCAT('%', #{keyword}, '%')"
            + "      OR c.barcode LIKE CONCAT('%', #{keyword}, '%'))"
            + "  </if>"
            + "  <if test='start != null'>"
            + "    AND <choose>"
            + "      <when test='\"PAID\".equals(dateField)'>p.paid_at</when>"
            + "      <otherwise>p.created_at</otherwise>"
            + "    </choose> &gt;= #{start}"
            + "  </if>"
            + "  <if test='endExclusive != null'>"
            + "    AND <choose>"
            + "      <when test='\"PAID\".equals(dateField)'>p.paid_at</when>"
            + "      <otherwise>p.created_at</otherwise>"
            + "    </choose> &lt; #{endExclusive}"
            + "  </if>"
            + "</where>"
            + " ORDER BY p.id DESC"
            + "</script>")
    IPage<PenaltyResponse> selectMyDetailPage(IPage<PenaltyResponse> page,
                                              @Param("readerId") Long readerId,
                                              @Param("status") PenaltyStatus status,
                                              @Param("keyword") String keyword,
                                              @Param("dateField") String dateField,
                                              @Param("start") LocalDateTime start,
                                              @Param("endExclusive") LocalDateTime endExclusive);
}
