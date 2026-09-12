package com.school.library.service;

import com.school.library.common.PageResult;
import com.school.library.dto.BookCopyResponse;
import com.school.library.dto.BookResponse;
import com.school.library.dto.CreateBookCopyRequest;
import com.school.library.dto.CreateBookRequest;
import com.school.library.dto.ExcelImportResult;
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

    /** 符合筛选条件的全部图书 ID（跨页全选用），与列表接口同一套筛选参数 */
    java.util.List<Long> listBookIds(String keyword, String field, String category, String publisher,
                                     BookStatus status);

    /** 批量上架/下架，返回实际更新条数 */
    java.util.Map<String, Integer> batchUpdateStatus(java.util.List<Long> ids, BookStatus status);

    /** 批量删除：有副本在借的跳过，返回 {count 删除数, skipped 跳过数} */
    java.util.Map<String, Integer> batchDelete(java.util.List<Long> ids);

    /** 全部图书分类（去重排序），供前端下拉筛选 */
    List<String> getCategories();

    /** 全部出版社（去重排序），供前端下拉筛选 */
    List<String> getPublishers();

    Book getBook(Long id);

    void deleteBook(Long id);

    Book findByIsbn(String isbn);

    BookCopy addBookCopy(Long bookId, CreateBookCopyRequest request);

    List<BookCopy> getBookCopies(Long bookId);

    /** Excel 批量导入书目：ISBN 存在则更新，不存在则新增；返回成功/失败统计与逐行错误 */
    ExcelImportResult importBooks(MultipartFile file);

    /** 导出图书为 xlsx（scope: all 全部 / page 单页 / selected 选中），文件内容与导入模板同款式 */
    byte[] exportBooks(String scope, String keyword, String field, String category, String publisher,
                       BookStatus status, int page, int size, java.util.List<Long> ids);

    /** 实体转响应对象（含副本统计） */
    BookResponse toResponse(Book book);

    /** 批量实体转响应对象（含副本统计，一次聚合查询，避免 N+1） */
    List<BookResponse> toResponses(List<Book> books);

    /** 副本实体转响应对象 */
    BookCopyResponse toCopyResponse(BookCopy copy);
}
