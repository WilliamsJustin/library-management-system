package com.school.library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.library.common.PageResult;
import com.school.library.common.Pages;
import com.school.library.dto.BookCopyResponse;
import com.school.library.dto.BookCopyStat;
import com.school.library.dto.BookResponse;
import com.school.library.dto.CreateBookCopyRequest;
import com.school.library.dto.CreateBookRequest;
import com.school.library.dto.UpdateBookRequest;
import com.school.library.entity.Book;
import com.school.library.entity.BookCopy;
import com.school.library.entity.BookStatus;
import com.school.library.entity.CopyStatus;
import com.school.library.exception.ConflictException;
import com.school.library.exception.NotFoundException;
import com.school.library.mapper.BookCopyMapper;
import com.school.library.mapper.BookMapper;
import com.school.library.service.BookService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl implements BookService {

    private final BookMapper bookMapper;
    private final BookCopyMapper copyMapper;

    public BookServiceImpl(BookMapper bookMapper, BookCopyMapper copyMapper) {
        this.bookMapper = bookMapper;
        this.copyMapper = copyMapper;
    }

    @Override
    @Transactional
    public Book createBook(CreateBookRequest request) {
        // 检查 ISBN 是否已存在
        if (bookMapper.selectCount(Wrappers.<Book>lambdaQuery().eq(Book::getIsbn, request.getIsbn())) > 0) {
            throw new ConflictException("ISBN 已存在");
        }

        Book book = new Book();
        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setCategory(request.getCategory());
        book.setStatus(BookStatus.ACTIVE);

        bookMapper.insert(book);
        return book;
    }

    @Override
    @Transactional
    public Book updateBook(Long id, UpdateBookRequest request) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new NotFoundException("图书不存在");
        }

        // 如果更新 ISBN，检查是否已存在
        if (request.getIsbn() != null && !request.getIsbn().equals(book.getIsbn())) {
            if (bookMapper.selectCount(Wrappers.<Book>lambdaQuery()
                    .eq(Book::getIsbn, request.getIsbn()).ne(Book::getId, id)) > 0) {
                throw new ConflictException("ISBN 已存在");
            }
            book.setIsbn(request.getIsbn());
        }

        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }
        if (request.getAuthor() != null) {
            book.setAuthor(request.getAuthor());
        }
        if (request.getPublisher() != null) {
            book.setPublisher(request.getPublisher());
        }
        if (request.getCategory() != null) {
            book.setCategory(request.getCategory());
        }

        bookMapper.updateById(book);
        return book;
    }

    @Override
    @Transactional
    public void toggleStatus(Long id, Boolean status) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new NotFoundException("图书不存在");
        }

        // 检查是否有副本正在借阅
        if (!Boolean.TRUE.equals(status) && hasBorrowedCopy(id)) {
            throw new ConflictException("图书有副本正在借阅，无法下架");
        }

        book.setStatus(Boolean.TRUE.equals(status) ? BookStatus.ACTIVE : BookStatus.INACTIVE);
        bookMapper.updateById(book);
    }

    /**
     * 图书列表。
     * 检索规则的优先级与原实现保持一致：
     * 1) 关键词 + 指定字段（首页检索框左侧下拉）时，只按该字段过滤，忽略分类/出版社/状态；
     * 2) 带出版社条件时，关键词同时匹配 书名/作者/ISBN/分类/出版社；
     * 3) 其余情况关键词只匹配 书名/作者/ISBN/分类。
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<BookResponse> getBooks(String keyword, String field, String category, String publisher,
                                             BookStatus status, int page, int size) {
        String f = (field == null) ? "" : field.trim();
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasField = !f.isEmpty() && !"any".equalsIgnoreCase(f);
        boolean hasCategory = category != null && !category.trim().isEmpty();
        boolean hasStatus = status != null;
        boolean hasPublisher = publisher != null && !publisher.trim().isEmpty();

        LambdaQueryWrapper<Book> wrapper = Wrappers.lambdaQuery();

        if (hasKeyword && hasField) {
            // 指定字段检索优先，命中后不再叠加其它条件
            switch (f.toLowerCase()) {
                case "title" -> wrapper.like(Book::getTitle, keyword);
                case "author" -> wrapper.like(Book::getAuthor, keyword);
                case "isbn" -> wrapper.like(Book::getIsbn, keyword);
                case "publisher" -> wrapper.like(Book::getPublisher, keyword);
                case "subject" -> wrapper.like(Book::getCategory, keyword);
                default -> wrapper.and(w -> w.like(Book::getTitle, keyword)
                        .or().like(Book::getAuthor, keyword)
                        .or().like(Book::getIsbn, keyword)
                        .or().like(Book::getCategory, keyword));
            }
        } else if (hasPublisher) {
            // 出版社筛选：与关键词/分类/状态任意组合
            if (hasKeyword) {
                wrapper.and(w -> w.like(Book::getTitle, keyword)
                        .or().like(Book::getAuthor, keyword)
                        .or().like(Book::getIsbn, keyword)
                        .or().like(Book::getCategory, keyword)
                        .or().like(Book::getPublisher, keyword));
            }
            wrapper.like(Book::getPublisher, publisher)
                    .eq(hasCategory, Book::getCategory, category)
                    .eq(hasStatus, Book::getStatus, status);
        } else {
            if (hasKeyword) {
                wrapper.and(w -> w.like(Book::getTitle, keyword)
                        .or().like(Book::getAuthor, keyword)
                        .or().like(Book::getIsbn, keyword)
                        .or().like(Book::getCategory, keyword));
            }
            wrapper.eq(hasCategory, Book::getCategory, category)
                    .eq(hasStatus, Book::getStatus, status);
        }

        wrapper.orderByAsc(Book::getId);
        IPage<Book> result = bookMapper.selectPage(Pages.of(page, size), wrapper);
        return PageResult.of(result, buildBookResponses(result.getRecords()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return bookMapper.selectDistinctCategories();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getPublishers() {
        return bookMapper.selectDistinctPublishers();
    }

    @Override
    @Transactional(readOnly = true)
    public Book getBook(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new NotFoundException("图书不存在");
        }
        return book;
    }

    @Override
    @Transactional
    public void deleteBook(Long id) {
        if (bookMapper.selectById(id) == null) {
            throw new NotFoundException("图书不存在");
        }

        // 检查是否有副本正在借阅
        if (hasBorrowedCopy(id)) {
            throw new ConflictException("图书有副本正在借阅，无法删除");
        }

        bookMapper.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Book findByIsbn(String isbn) {
        return bookMapper.selectOne(Wrappers.<Book>lambdaQuery().eq(Book::getIsbn, isbn));
    }

    @Override
    @Transactional
    public BookCopy addBookCopy(Long bookId, CreateBookCopyRequest request) {
        if (bookMapper.selectById(bookId) == null) {
            throw new NotFoundException("图书不存在");
        }

        if (copyMapper.selectCount(Wrappers.<BookCopy>lambdaQuery()
                .eq(BookCopy::getBarcode, request.getBarcode())) > 0) {
            throw new ConflictException("条形码已存在");
        }

        BookCopy copy = new BookCopy();
        copy.setBookId(bookId);
        copy.setBarcode(request.getBarcode());
        copy.setLocation(request.getLocation());
        copy.setStatus(CopyStatus.IN_STOCK);

        copyMapper.insert(copy);
        return copy;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookCopy> getBookCopies(Long bookId) {
        return copyMapper.selectList(Wrappers.<BookCopy>lambdaQuery()
                .eq(BookCopy::getBookId, bookId)
                .orderByAsc(BookCopy::getId));
    }

    @Override
    @Transactional
    public List<Book> importBooks(MultipartFile file) {
        List<Book> books = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // 跳过标题行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String isbn = getCellValue(row.getCell(0));
                String title = getCellValue(row.getCell(1));
                String author = getCellValue(row.getCell(2));
                String publisher = getCellValue(row.getCell(3));
                String category = getCellValue(row.getCell(4));

                if (isbn != null && !isbn.trim().isEmpty()) {
                    // 存在则更新，不存在则新增
                    Book existing = findByIsbn(isbn);
                    if (existing == null) {
                        Book book = new Book();
                        book.setIsbn(isbn);
                        book.setTitle(title);
                        book.setAuthor(author);
                        book.setPublisher(publisher);
                        book.setCategory(category);
                        book.setStatus(BookStatus.ACTIVE);
                        bookMapper.insert(book);
                        books.add(book);
                    } else {
                        existing.setTitle(title);
                        existing.setAuthor(author);
                        existing.setPublisher(publisher);
                        existing.setCategory(category);
                        bookMapper.updateById(existing);
                        books.add(existing);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }

        return books;
    }

    @Override
    @Transactional(readOnly = true)
    public BookResponse toResponse(Book book) {
        BookResponse response = BookResponse.fromEntity(book);
        response.setTotalCopies(copyMapper.selectCount(Wrappers.<BookCopy>lambdaQuery()
                .eq(BookCopy::getBookId, book.getId())));
        response.setAvailableCopies(copyMapper.selectCount(Wrappers.<BookCopy>lambdaQuery()
                .eq(BookCopy::getBookId, book.getId())
                .eq(BookCopy::getStatus, CopyStatus.IN_STOCK)));
        return response;
    }

    @Override
    public BookCopyResponse toCopyResponse(BookCopy copy) {
        return BookCopyResponse.fromEntity(copy);
    }

    /** 批量装载响应对象：一次聚合查询拿到所有书的副本统计，避免逐本查两次 count */
    private List<BookResponse> buildBookResponses(List<Book> books) {
        if (books.isEmpty()) {
            return List.of();
        }
        List<Long> bookIds = books.stream().map(Book::getId).toList();
        Map<Long, BookCopyStat> stats = copyMapper.selectStatsByBookIds(bookIds).stream()
                .collect(Collectors.toMap(BookCopyStat::getBookId, Function.identity()));

        return books.stream().map(book -> {
            BookResponse response = BookResponse.fromEntity(book);
            BookCopyStat stat = stats.get(book.getId());
            response.setTotalCopies(stat == null ? 0L : stat.getTotalCopies());
            response.setAvailableCopies(stat == null ? 0L : stat.getAvailableCopies());
            return response;
        }).toList();
    }

    private boolean hasBorrowedCopy(Long bookId) {
        return copyMapper.selectCount(Wrappers.<BookCopy>lambdaQuery()
                .eq(BookCopy::getBookId, bookId)
                .eq(BookCopy::getStatus, CopyStatus.BORROWED)) > 0;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    String value = String.valueOf((long) cell.getNumericCellValue());
                    if (value.endsWith(".0")) {
                        return value.substring(0, value.length() - 2);
                    }
                    return value;
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getCellFormula();
                } catch (Exception e) {
                    return null;
                }
            default:
                return null;
        }
    }
}
