package com.school.library.repository;

import com.school.library.entity.Penalty;
import com.school.library.entity.PenaltyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    boolean existsByLoanId(Long loanId);

    long countByReaderIdAndStatus(Long readerId, PenaltyStatus status);

    Page<Penalty> findByReaderIdOrderByIdDesc(Long readerId, Pageable pageable);

    @Query("""
            SELECT p FROM Penalty p
            WHERE (:status IS NULL OR p.status = :status)
            ORDER BY p.id DESC
            """)
    Page<Penalty> query(@Param("status") PenaltyStatus status, Pageable pageable);
}
