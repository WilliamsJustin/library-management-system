package com.school.library.service;

import com.school.library.common.PageResult;
import com.school.library.dto.BookCopyResponse;
import com.school.library.dto.BookResponse;
import com.school.library.dto.CreateBookCopyRequest;
import com.school.library.dto.CreateBookRequest;
import com.school.library.dto.UpdateBookRequest;
import com.school.library.entity.Book;
import com.school.library.entity.BookCopy;
import com.school.library.entity.BookStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BookService {

    Book createBook(CreateBookRequest request);

    Book updateBook(Long id, UpdateBookRequest request);

    void toggleStatus(Long id, Boolean status);

    /** 图书列表（关键词/字段/分类/出版社/状态组合筛选），page 为 0 基 */
    PageResult<BookResponse> getBooks(String keyword, String field, String category, String publisher,
                                      BookStatus status, int page, int size);

    /** 全部图书分类（去重排序），供前端下拉筛选 */
    List<String> getCategories();

    /** 全部出版社（去重排序），供前端下拉筛选 */
    List<String> getPublishers();

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
