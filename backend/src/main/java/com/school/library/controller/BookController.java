package com.school.library.controller;

import com.school.library.common.PageResult;
import com.school.library.dto.BatchBookStatusRequest;
import com.school.library.dto.BatchIdsRequest;
import com.school.library.dto.BookCopyResponse;
import com.school.library.dto.BookResponse;
import com.school.library.dto.CreateBookCopyRequest;
import com.school.library.dto.CreateBookRequest;
import com.school.library.dto.ExcelImportResult;
import com.school.library.dto.StatusRequest;
import com.school.library.dto.UpdateBookRequest;
import com.school.library.entity.Book;
import com.school.library.entity.BookCopy;
import com.school.library.entity.BookStatus;
import com.school.library.service.BookService;
import com.school.library.service.impl.BookServiceImpl;
import com.school.library.util.ExcelTemplateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@Tag(name = "图书编目", description = "图书信息管理接口")
public class BookController {

    @Autowired
    private BookService bookService;

    @Operation(summary = "创建图书")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        Book book = bookService.createBook(request);
        return ResponseEntity.ok(bookService.toResponse(book));
    }

    @Operation(summary = "更新图书信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookRequest request) {
        Book book = bookService.updateBook(id, request);
        return ResponseEntity.ok(bookService.toResponse(book));
    }

    @Operation(summary = "上架/下架图书")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleBookStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest request) {
        bookService.toggleStatus(id, request.getStatus());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "查询图书列表（公共，无需登录）")
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<PageResult<BookResponse>> getBooks(
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword,
            @Parameter(description = "检索字段：any/title/author/isbn/publisher/subject") @RequestParam(required = false) String field,
            @Parameter(description = "分类") @RequestParam(required = false) String category,
            @Parameter(description = "出版社（模糊匹配）") @RequestParam(required = false) String publisher,
            @Parameter(description = "状态") @RequestParam(required = false) BookStatus status,
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookService.getBooks(keyword, field, category, publisher, status, page, size));
    }

    @Operation(summary = "获取全部图书分类（公共，无需登录）")
    @GetMapping("/categories")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(bookService.getCategories());
    }

    @Operation(summary = "获取全部出版社（公共，无需登录）")
    @GetMapping("/publishers")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<String>> getPublishers() {
        return ResponseEntity.ok(bookService.getPublishers());
    }

    @Operation(summary = "获取图书详情（公共，无需登录）")
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<BookResponse> getBook(@PathVariable Long id) {
        Book book = bookService.getBook(id);
        return ResponseEntity.ok(bookService.toResponse(book));
    }

    @Operation(summary = "删除图书")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "添加图书副本")
    @PostMapping("/{id}/copies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookCopyResponse> addBookCopy(
            @PathVariable Long id,
            @Valid @RequestBody CreateBookCopyRequest request) {
        BookCopy copy = bookService.addBookCopy(id, request);
        return ResponseEntity.ok(bookService.toCopyResponse(copy));
    }

    @Operation(summary = "查询图书副本")
    @GetMapping("/{id}/copies")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<List<BookCopyResponse>> getBookCopies(@PathVariable Long id) {
        List<BookCopy> copies = bookService.getBookCopies(id);
        List<BookCopyResponse> response = copies.stream()
                .map(bookService::toCopyResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "符合筛选条件的全部图书 ID（跨页全选用，管理员）")
    @GetMapping("/ids")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Long>> listBookIds(
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword,
            @Parameter(description = "检索字段：any/title/author/isbn/publisher/subject") @RequestParam(required = false) String field,
            @Parameter(description = "分类") @RequestParam(required = false) String category,
            @Parameter(description = "出版社（模糊匹配）") @RequestParam(required = false) String publisher,
            @Parameter(description = "状态") @RequestParam(required = false) BookStatus status) {
        return ResponseEntity.ok(bookService.listBookIds(keyword, field, category, publisher, status));
    }

    @Operation(summary = "批量上架/下架（管理员），返回实际更新条数")
    @PatchMapping("/batch/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String, Integer>> batchUpdateStatus(
            @Valid @RequestBody BatchBookStatusRequest request) {
        return ResponseEntity.ok(bookService.batchUpdateStatus(request.ids(), request.status()));
    }

    @Operation(summary = "批量删除图书（管理员）：有副本在借的跳过，返回删除数与跳过数")
    @PostMapping("/batch/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String, Integer>> batchDelete(
            @Valid @RequestBody BatchIdsRequest request) {
        return ResponseEntity.ok(bookService.batchDelete(request.ids()));
    }

    @Operation(summary = "批量导入图书（列顺序 ISBN|书名|作者|出版社|分类；ISBN 存在则更新）")
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExcelImportResult> importBooks(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(bookService.importBooks(file));
    }

    @Operation(summary = "下载图书导入模板（管理员）：最左列为图片链接，可填链接或留空（留空=清除封面）")
    @GetMapping("/import/template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> importTemplate() {
        byte[] bytes = ExcelTemplateUtil.build(
                BookServiceImpl.EXPORT_HEADERS,
                List.<String[]>of(
                        new String[]{"https://example.com/cover-a.jpg", "978-7-111-40701-0", "算法导论",
                                "Thomas H. Cormen", "机械工业出版社", "计算机"},
                        new String[]{"", "978-7-115-42802-8", "深入浅出MySQL", "姜承尧", "人民邮电出版社", "计算机"}),
                BookServiceImpl.EXPORT_WIDTHS);
        return ExcelTemplateUtil.toResponse(bytes, BookServiceImpl.TEMPLATE_FILENAME);
    }

    @Operation(summary = "导出图书（管理员）：scope=all 全部 / page 单页 / selected 选中（ids 逗号分隔），文件与导入模板同款式")
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportBooks(
            @Parameter(description = "导出范围：all / page / selected") @RequestParam(defaultValue = "all") String scope,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword,
            @Parameter(description = "检索字段") @RequestParam(required = false) String field,
            @Parameter(description = "分类") @RequestParam(required = false) String category,
            @Parameter(description = "出版社（模糊匹配）") @RequestParam(required = false) String publisher,
            @Parameter(description = "状态") @RequestParam(required = false) BookStatus status,
            @Parameter(description = "页码，scope=page 时使用") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数，scope=page 时使用") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "选中的图书 ID，逗号分隔，scope=selected 时使用") @RequestParam(required = false) String ids) {
        byte[] bytes = bookService.exportBooks(scope, keyword, field, category, publisher, status,
                page, size, parseIds(ids));
        return ExcelTemplateUtil.toResponse(bytes, BookServiceImpl.EXPORT_FILENAME);
    }

    /** "1,2,3" -> [1L, 2L, 3L] */
    private static List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) return List.of();
        return java.util.Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
    }
}
