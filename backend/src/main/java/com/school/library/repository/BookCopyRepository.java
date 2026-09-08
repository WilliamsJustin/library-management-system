package com.school.library.repository;

import com.school.library.entity.BookCopy;
import com.school.library.entity.CopyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    Optional<BookCopy> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    List<BookCopy> findByBookId(Long bookId);

    long countByBookIdAndStatus(Long bookId, CopyStatus status);

    long countByBookId(Long bookId);

    boolean existsByBookIdAndStatus(Long bookId, CopyStatus status);
}
