package com.school.library.service.impl;

import com.school.library.dto.BookCopyResponse;
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
import com.school.library.repository.BookCopyRepository;
import com.school.library.repository.BookRepository;
import com.school.library.service.BookService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository copyRepository;

    @Override
    @Transactional
    public Book createBook(CreateBookRequest request) {
        // 检查 ISBN 是否已存在
        if (bookRepository.findByIsbn(request.getIsbn()) != null) {
            throw new ConflictException("ISBN 已存在");
        }

        Book book = new Book();
        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setCategory(request.getCategory());
        book.setStatus(BookStatus.ACTIVE);

        return bookRepository.save(book);
    }

    @Override
    @Transactional
    public Book updateBook(Long id, UpdateBookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("图书不存在"));

        // 如果更新 ISBN，检查是否已存在
        if (request.getIsbn() != null && !request.getIsbn().equals(book.getIsbn())) {
            if (bookRepository.findByIsbn(request.getIsbn()) != null) {
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

        return bookRepository.save(book);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id, Boolean status) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("图书不存在"));

        // 检查是否有副本正在借阅
        if (!status && copyRepository.existsByBookIdAndStatus(id, CopyStatus.BORROWED)) {
            throw new ConflictException("图书有副本正在借阅，无法下架");
        }

        book.setStatus(status ? BookStatus.ACTIVE : BookStatus.INACTIVE);
        bookRepository.save(book);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Book> getBooks(String keyword, String field, String category, BookStatus status, Pageable pageable) {
        String f = (field == null) ? "" : field.trim();
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasField = !f.isEmpty() && !"any".equalsIgnoreCase(f);
        boolean hasCategory = category != null && !category.trim().isEmpty();
        boolean hasStatus = status != null;

        // 指定字段检索优先（首页检索框左侧下拉）
        if (hasKeyword && hasField) {
            return switch (f.toLowerCase()) {
                case "title" -> bookRepository.searchByTitle(keyword, pageable);
                case "author" -> bookRepository.searchByAuthor(keyword, pageable);
                case "isbn" -> bookRepository.searchByIsbn(keyword, pageable);
                case "publisher" -> bookRepository.searchByPublisher(keyword, pageable);
                case "subject" -> bookRepository.searchBySubject(keyword, pageable);
                default -> bookRepository.search(keyword, pageable);
            };
        }

        // 通用检索 / 分类 / 状态
        if (hasKeyword) {
            if (hasCategory && hasStatus) {
                return bookRepository.searchByKeywordAndCategoryAndStatus(keyword, category, status, pageable);
            } else if (hasCategory) {
                return bookRepository.searchByKeywordAndCategory(keyword, category, pageable);
            } else if (hasStatus) {
                return bookRepository.searchByKeywordAndStatus(keyword, status, pageable);
            } else {
                return bookRepository.search(keyword, pageable);
            }
        } else if (hasCategory && hasStatus) {
            return bookRepository.findByCategoryAndStatus(category, status, pageable);
        } else if (hasCategory) {
            return bookRepository.findByCategory(category, pageable);
        } else if (hasStatus) {
            return bookRepository.findByStatus(status, pageable);
        } else {
            return bookRepository.findAll(pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Book getBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("图书不存在"));
    }

    @Override
    @Transactional
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("图书不存在"));

        // 检查是否有副本正在借阅
        if (copyRepository.existsByBookIdAndStatus(id, CopyStatus.BORROWED)) {
            throw new ConflictException("图书有副本正在借阅，无法删除");
        }

        bookRepository.delete(book);
    }

    @Override
    @Transactional(readOnly = true)
    public Book findByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }

    @Override
    @Transactional
    public BookCopy addBookCopy(Long bookId, CreateBookCopyRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException("图书不存在"));

        if (copyRepository.existsByBarcode(request.getBarcode())) {
            throw new ConflictException("条形码已存在");
        }

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setBarcode(request.getBarcode());
        copy.setLocation(request.getLocation());
        copy.setStatus(CopyStatus.IN_STOCK);

        return copyRepository.save(copy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookCopy> getBookCopies(Long bookId) {
        return copyRepository.findByBookId(bookId);
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
                    Book existing = bookRepository.findByIsbn(isbn);
                    if (existing == null) {
                        Book book = new Book();
                        book.setIsbn(isbn);
                        book.setTitle(title);
                        book.setAuthor(author);
                        book.setPublisher(publisher);
                        book.setCategory(category);
                        book.setStatus(BookStatus.ACTIVE);
                        book = bookRepository.save(book);
                        books.add(book);
                    } else {
                        existing.setTitle(title);
                        existing.setAuthor(author);
                        existing.setPublisher(publisher);
                        existing.setCategory(category);
                        bookRepository.save(existing);
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
        response.setTotalCopies(copyRepository.countByBookId(book.getId()));
        response.setAvailableCopies(copyRepository.countByBookIdAndStatus(book.getId(), CopyStatus.IN_STOCK));
        return response;
    }

    @Override
    public BookCopyResponse toCopyResponse(BookCopy copy) {
        return BookCopyResponse.fromEntity(copy);
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
