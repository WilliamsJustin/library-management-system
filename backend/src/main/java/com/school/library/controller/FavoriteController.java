package com.school.library.controller;

import com.school.library.common.PageResult;
import com.school.library.dto.BookResponse;
import com.school.library.dto.FavoriteStateResponse;
import com.school.library.security.AppPrincipal;
import com.school.library.security.CurrentUser;
import com.school.library.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@Tag(name = "我的收藏", description = "读者收藏书目")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @Operation(summary = "我的收藏（分页，含副本统计；支持关键词/字段检索）")
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<PageResult<BookResponse>> myFavorites(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "检索字段：any 任意词 / title 题名 / author 著者 / isbn / publisher 出版社 / subject 主题")
            @RequestParam(required = false) String field,
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        AppPrincipal caller = CurrentUser.get();
        return ResponseEntity.ok(favoriteService.myFavorites(caller, keyword, field, page, size));
    }

    @Operation(summary = "我收藏的图书ID列表（供前台标记已收藏状态）")
    @GetMapping("/my/ids")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<List<Long>> myFavoriteIds() {
        AppPrincipal caller = CurrentUser.get();
        return ResponseEntity.ok(favoriteService.myFavoriteBookIds(caller));
    }

    @Operation(summary = "收藏图书（重复收藏幂等）")
    @PostMapping("/{bookId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<FavoriteStateResponse> add(@PathVariable Long bookId) {
        AppPrincipal caller = CurrentUser.get();
        boolean favorited = favoriteService.add(caller, bookId);
        return ResponseEntity.ok(new FavoriteStateResponse(bookId, favorited));
    }

    @Operation(summary = "取消收藏（幂等）")
    @DeleteMapping("/{bookId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<FavoriteStateResponse> remove(@PathVariable Long bookId) {
        AppPrincipal caller = CurrentUser.get();
        boolean favorited = favoriteService.remove(caller, bookId);
        return ResponseEntity.ok(new FavoriteStateResponse(bookId, favorited));
    }

    @Operation(summary = "查询某本书是否已收藏")
    @GetMapping("/{bookId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<FavoriteStateResponse> status(@PathVariable Long bookId) {
        AppPrincipal caller = CurrentUser.get();
        return ResponseEntity.ok(new FavoriteStateResponse(bookId, favoriteService.isFavorited(caller, bookId)));
    }
}
