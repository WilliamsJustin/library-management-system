package com.school.library.repository;

import com.school.library.entity.Book;
import com.school.library.entity.BookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByIsbn(String isbn);

    Book findByIsbn(String isbn);

    @Query("""
            SELECT b FROM Book b
            WHERE (:keyword IS NULL OR :keyword = ''
                OR b.title LIKE %:keyword%
                OR b.author LIKE %:keyword%
                OR b.isbn LIKE %:keyword%
                OR b.category LIKE %:keyword%)
            """)
    Page<Book> search(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT b FROM Book b
            WHERE (:keyword IS NULL OR :keyword = '' OR b.title LIKE %:keyword%)
            """)
    Page<Book> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT b FROM Book b
            WHERE (:keyword IS NULL OR :keyword = '' OR b.author LIKE %:keyword%)
            """)
    Page<Book> searchByAuthor(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT b FROM Book b
            WHERE (:keyword IS NULL OR :keyword = '' OR b.isbn LIKE %:keyword%)
            """)
    Page<Book> searchByIsbn(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT b FROM Book b
            WHERE (:keyword IS NULL OR :keyword = '' OR b.publisher LIKE %:keyword%)
            """)
    Page<Book> searchByPublisher(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT b FROM Book b
            WHERE (:keyword IS NULL OR :keyword = '' OR b.category LIKE %:keyword%)
            """)
    Page<Book> searchBySubject(@Param("keyword") String keyword, Pageable pageable);

    Page<Book> findByCategory(String category, Pageable pageable);

    Page<Book> findByStatus(BookStatus status, Pageable pageable);

    Page<Book> findByCategoryAndStatus(String category, BookStatus status, Pageable pageable);

    @Query("""
            SELECT b FROM Book b
            WHERE (:keyword IS NULL OR :keyword = ''
                OR b.title LIKE %:keyword%
                OR b.author LIKE %:keyword%
                OR b.isbn LIKE %:keyword%
                OR b.category LIKE %:keyword%)
                AND (:category IS NULL OR :category = '' OR b.category = :category)
            """)
    Page<Book> searchByKeywordAndCategory(@Param("keyword") String keyword, @Param("category") String category, Pageable pageable);

    @Query("""
            SELECT b FROM Book b
            WHERE (:keyword IS NULL OR :keyword = ''
                OR b.title LIKE %:keyword%
                OR b.author LIKE %:keyword%
                OR b.isbn LIKE %:keyword%
                OR b.category LIKE %:keyword%)
                AND (:status IS NULL OR b.status = :status)
            """)
    Page<Book> searchByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") BookStatus status, Pageable pageable);

    @Query("""
            SELECT b FROM Book b
            WHERE (:keyword IS NULL OR :keyword = ''
                OR b.title LIKE %:keyword%
                OR b.author LIKE %:keyword%
                OR b.isbn LIKE %:keyword%
                OR b.category LIKE %:keyword%)
                AND (:category IS NULL OR :category = '' OR b.category = :category)
                AND (:status IS NULL OR b.status = :status)
            """)
    Page<Book> searchByKeywordAndCategoryAndStatus(@Param("keyword") String keyword, @Param("category") String category, @Param("status") BookStatus status, Pageable pageable);
}
