package com.school.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.library.dto.LoanResponse;
import com.school.library.entity.Loan;
import com.school.library.entity.LoanStatus;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 借阅 Mapper。
 *
 * <p>{@link Loan} 实体只持有 copyId / readerId，不再像 JPA 那样能通过
 * {@code loan.getCopy().getBook().getTitle()} 跨表导航，所以「借阅明细」统一由下面的
 * join 查询一次查全（列名用下划线，靠 map-underscore-to-camel-case 自动映射到 DTO 的驼峰属性）。
 */
public interface LoanMapper extends BaseMapper<Loan> {

    /** 借阅明细的公共 SELECT 片段 */
    String DETAIL_SELECT = """
            SELECT l.id            AS id,
                   l.borrowed_at   AS borrowed_at,
                   l.due_date      AS due_date,
                   l.returned_at   AS returned_at,
                   l.renewed_count AS renewed_count,
                   l.status        AS status,
                   b.id            AS book_id,
                   b.title         AS book_title,
                   b.isbn          AS isbn,
                   c.barcode       AS barcode,
                   r.id            AS reader_id,
                   r.name          AS reader_name,
                   r.account       AS reader_account
            FROM loan l
            JOIN book_copy c ON c.id = l.copy_id
            JOIN book b ON b.id = c.book_id
            JOIN reader r ON r.id = l.reader_id
            """;

    /** 按 ID 查一条借阅明细（借出/归还/续借后返回给前端用） */
    @Select(DETAIL_SELECT + " WHERE l.id = #{id}")
    LoanResponse selectDetailById(@Param("id") Long id);

    /** 借阅查询分页：readerId / status 均可为 null（表示不限制），按 ID 倒序 */
    @Select("<script>"
            + DETAIL_SELECT
            + "<where>"
            + "  <if test='readerId != null'>AND l.reader_id = #{readerId}</if>"
            + "  <if test='status != null'>AND l.status = #{status}</if>"
            + "</where>"
            + " ORDER BY l.id DESC"
            + "</script>")
    IPage<LoanResponse> selectDetailPage(IPage<LoanResponse> page,
                                         @Param("readerId") Long readerId,
                                         @Param("status") LoanStatus status);

    /** 取借阅对应的书名，用于拼装逾期提醒文案 */
    @Select("""
            SELECT b.title
            FROM loan l
            JOIN book_copy c ON c.id = l.copy_id
            JOIN book b ON b.id = c.book_id
            WHERE l.id = #{loanId}
            """)
    String selectBookTitleByLoanId(@Param("loanId") Long loanId);
}
