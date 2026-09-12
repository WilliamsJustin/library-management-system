package com.school.library.service;

import com.school.library.common.PageResult;
import com.school.library.dto.BookResponse;
import com.school.library.security.AppPrincipal;

import java.util.List;

/** 读者收藏 */
public interface FavoriteService {

    /**
     * 我的收藏（分页，按收藏时间倒序）。
     * keyword/field 与书目检索语义一致：any 任意词、title 题名、author 著者、
     * isbn、publisher 出版社、subject 主题（分类）；keyword 为空时不过滤。
     */
    PageResult<BookResponse> myFavorites(AppPrincipal caller, String keyword, String field, int page, int size);

    /** 我收藏的全部图书 ID（前台标记「已收藏」用） */
    List<Long> myFavoriteBookIds(AppPrincipal caller);

    /** 收藏某本书（重复收藏幂等，返回收藏后的状态） */
    boolean add(AppPrincipal caller, Long bookId);

    /** 取消收藏（未收藏时也返回成功，幂等） */
    boolean remove(AppPrincipal caller, Long bookId);

    /** 是否已收藏 */
    boolean isFavorited(AppPrincipal caller, Long bookId);
}
