package com.school.library.repository;

import com.school.library.entity.Reader;
import com.school.library.entity.ReaderStatus;
import com.school.library.entity.ReaderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReaderRepository extends JpaRepository<Reader, Long> {

    Optional<Reader> findByAccount(String account);

    boolean existsByAccount(String account);

    boolean existsByStudentNo(String studentNo);

    boolean existsByAccountAndIdNot(String account, Long id);

    boolean existsByStudentNoAndIdNot(String studentNo, Long id);

    /** 关键词 + 类型 + 状态 组合查询（参数均可选） */
    @Query("""
            SELECT r FROM Reader r
            WHERE (:keyword IS NULL OR :keyword = ''
                OR r.name LIKE %:keyword%
                OR r.account LIKE %:keyword%
                OR r.studentNo LIKE %:keyword%)
              AND (:type IS NULL OR r.type = :type)
              AND (:status IS NULL OR r.status = :status)
            """)
    Page<Reader> query(@Param("keyword") String keyword,
                       @Param("type") ReaderType type,
                       @Param("status") ReaderStatus status,
                       Pageable pageable);
}
