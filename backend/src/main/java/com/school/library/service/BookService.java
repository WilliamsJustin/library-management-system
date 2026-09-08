package com.school.library.service;

import com.school.library.dto.BookCopyResponse;
import com.school.library.dto.BookResponse;
import com.school.library.dto.CreateBookCopyRequest;
import com.school.library.dto.CreateBookRequest;
import com.school.library.dto.UpdateBookRequest;
import com.school.library.entity.Book;
import com.school.library.entity.BookCopy;
import com.school.library.entity.BookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BookService {

    Book createBook(CreateBookRequest request);

    Book updateBook(Long id, UpdateBookRequest request);

    void toggleStatus(Long id, Boolean status);

    Page<Book> getBooks(String keyword, String category, BookStatus status, Pageable pageable);

    Book getBook(Long id);

    void deleteBook(Long id);

    Book findByIsbn(String isbn);

    BookCopy addBookCopy(Long bookId, CreateBookCopyRequest request);

    List<BookCopy> getBookCopies(Long bookId);

    List<Book> importBooks(MultipartFile file);

    /** 实体转响应对象（含副本统计） */
    BookResponse toResponse(Book book);

    /** 副本实体转响应对象 */
    BookCopyResponse toCopyResponse(BookCopy copy);
}
