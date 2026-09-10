package com.school.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.library.dto.PenaltyResponse;
import com.school.library.entity.Penalty;
import com.school.library.entity.PenaltyStatus;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
                   b.title      AS book_title,
                   c.barcode    AS barcode,
                   r.id         AS reader_id,
                   r.name       AS reader_name,
                   r.account    AS reader_account
            FROM penalty p
            JOIN loan l ON l.id = p.loan_id
            JOIN book_copy c ON c.id = l.copy_id
            JOIN book b ON b.id = c.book_id
            JOIN reader r ON r.id = p.reader_id
            """;

    @Select(DETAIL_SELECT + " WHERE p.id = #{id}")
    PenaltyResponse selectDetailById(@Param("id") Long id);

    /** 罚款查询分页：readerId / status 均可为 null（表示不限制），按 ID 倒序 */
    @Select("<script>"
            + DETAIL_SELECT
            + "<where>"
            + "  <if test='readerId != null'>AND p.reader_id = #{readerId}</if>"
            + "  <if test='status != null'>AND p.status = #{status}</if>"
            + "</where>"
            + " ORDER BY p.id DESC"
            + "</script>")
    IPage<PenaltyResponse> selectDetailPage(IPage<PenaltyResponse> page,
                                            @Param("readerId") Long readerId,
                                            @Param("status") PenaltyStatus status);
}
