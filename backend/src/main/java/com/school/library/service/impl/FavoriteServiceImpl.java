package com.school.library.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.library.common.PageResult;
import com.school.library.dto.BookResponse;
import com.school.library.entity.Book;
import com.school.library.entity.Favorite;
import com.school.library.exception.NotFoundException;
import com.school.library.mapper.BookMapper;
import com.school.library.mapper.FavoriteMapper;
import com.school.library.security.AppPrincipal;
import com.school.library.service.BookService;
import com.school.library.service.FavoriteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final BookMapper bookMapper;
    private final BookService bookService;

    public FavoriteServiceImpl(FavoriteMapper favoriteMapper, BookMapper bookMapper, BookService bookService) {
        this.favoriteMapper = favoriteMapper;
        this.bookMapper = bookMapper;
        this.bookService = bookService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BookResponse> myFavorites(AppPrincipal caller, String keyword, String field, int page, int size) {
        // 收藏量级很小（单个读者几十条以内），先按收藏时间倒序取全部书目，
        // 在内存里做关键词过滤与分页，省一条 join 查询
        List<BookResponse> all = loadBooksInOrder(favoriteMapper.selectBookIdsByReader(caller.userId()));

        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<BookResponse> filtered = kw.isEmpty() ? all
                : all.stream().filter(b -> matches(b, kw, field)).toList();

        // 手工分页（保持 PageResult 的 0 基语义）
        int from = (int) Math.min((long) Math.max(page, 0) * Math.max(size, 1), filtered.size());
        int to = Math.min(from + Math.max(size, 1), filtered.size());
        List<BookResponse> content = new ArrayList<>(filtered.subList(from, to));

        Page<BookResponse> result = new Page<>(page + 1, Math.max(size, 1));
        result.setTotal(filtered.size());
        result.setRecords(content);
        return PageResult.of(result);
    }

    /** 关键词按检索字段匹配（与书目检索的字段语义一致，全部不区分大小写） */
    private boolean matches(BookResponse book, String kw, String field) {
        String f = field == null ? "any" : field;
        return switch (f) {
            case "title" -> contains(book.getTitle(), kw);
            case "author" -> contains(book.getAuthor(), kw);
            case "isbn" -> contains(book.getIsbn(), kw);
            case "publisher" -> contains(book.getPublisher(), kw);
            case "subject" -> contains(book.getCategory(), kw);
            default -> contains(book.getTitle(), kw) || contains(book.getAuthor(), kw)
                    || contains(book.getIsbn(), kw) || contains(book.getPublisher(), kw)
                    || contains(book.getCategory(), kw);
        };
    }

    private boolean contains(String value, String kw) {
        return value != null && value.toLowerCase().contains(kw);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> myFavoriteBookIds(AppPrincipal caller) {
        return favoriteMapper.selectBookIdsByReader(caller.userId());
    }

    @Override
    @Transactional
    public boolean add(AppPrincipal caller, Long bookId) {
        if (bookMapper.selectById(bookId) == null) {
            throw new NotFoundException("图书不存在");
        }
        // 唯一键 uk_favorite_reader_book 兜底，这里先查一次避免把重复插入当异常抛给前端
        long exists = favoriteMapper.selectCount(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getReaderId, caller.userId())
                .eq(Favorite::getBookId, bookId));
        if (exists == 0) {
            favoriteMapper.insert(new Favorite(caller.userId(), bookId));
        }
        return true;
    }

    @Override
    @Transactional
    public boolean remove(AppPrincipal caller, Long bookId) {
        favoriteMapper.delete(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getReaderId, caller.userId())
                .eq(Favorite::getBookId, bookId));
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorited(AppPrincipal caller, Long bookId) {
        return favoriteMapper.selectCount(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getReaderId, caller.userId())
                .eq(Favorite::getBookId, bookId)) > 0;
    }

    /**
     * 按收藏顺序批量装载书目。
     * 一次 selectBatchIds 拿到整页数据（不再逐条查询），再按收藏时间顺序重排；
     * 被删掉的书目会自动跳过，副本统计由 bookService 一次性聚合。
     */
    private List<BookResponse> loadBooksInOrder(List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Book> byId = new LinkedHashMap<>();
        bookMapper.selectBatchIds(bookIds).forEach(book -> byId.put(book.getId(), book));

        List<Book> ordered = bookIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
        return bookService.toResponses(ordered);
    }
}
