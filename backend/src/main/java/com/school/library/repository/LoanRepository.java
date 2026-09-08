package com.school.library.repository;

import com.school.library.entity.Loan;
import com.school.library.entity.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    long countByReaderIdAndStatusIn(Long readerId, List<LoanStatus> statuses);

    Page<Loan> findByReaderIdOrderByIdDesc(Long readerId, Pageable pageable);

    boolean existsByCopyIdAndStatusIn(Long copyId, List<LoanStatus> statuses);

    List<Loan> findByStatusAndDueDateBefore(LoanStatus status, LocalDate date);

    List<Loan> findByStatusAndDueDateBetween(LoanStatus status, LocalDate start, LocalDate end);

    @Query("""
            SELECT l FROM Loan l
            WHERE (:readerId IS NULL OR l.reader.id = :readerId)
              AND (:copyId IS NULL OR l.copy.id = :copyId)
            ORDER BY l.id DESC
            """)
    Page<Loan> query(@Param("readerId") Long readerId, @Param("copyId") Long copyId, Pageable pageable);

    List<Loan> findByReaderId(Long readerId);

    boolean existsByReaderIdAndStatusIn(Long readerId, List<LoanStatus> statuses);

    @Query("""
            SELECT l FROM Loan l
            WHERE (:readerId IS NULL OR l.reader.id = :readerId)
              AND (:status IS NULL OR l.status = :status)
            ORDER BY l.id DESC
            """)
    Page<Loan> query(@Param("readerId") Long readerId, @Param("status") LoanStatus status, Pageable pageable);
}
