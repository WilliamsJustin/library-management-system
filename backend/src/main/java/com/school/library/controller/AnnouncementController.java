package com.school.library.controller;

import com.school.library.common.PageResult;
import com.school.library.dto.AnnouncementResponse;
import com.school.library.dto.CreateAnnouncementRequest;
import com.school.library.security.AppPrincipal;
import com.school.library.security.CurrentUser;
import com.school.library.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/announcements")
@Tag(name = "公告", description = "首页公告展示（公开）与管理（管理员/教师）")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @Operation(summary = "最新公告列表（公开，无需登录，分页；支持标题模糊与发布时间区间筛选）")
    @GetMapping
    public ResponseEntity<PageResult<AnnouncementResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        int pageSize = Math.min(Math.max(size, 1), 20);
        return ResponseEntity.ok(announcementService.list(keyword, startDate, endDate, page, pageSize));
    }

    @Operation(summary = "发布新公告（管理员或教师）")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnnouncementResponse> create(@Valid @RequestBody CreateAnnouncementRequest request) {
        AppPrincipal caller = CurrentUser.get();
        return ResponseEntity.ok(announcementService.create(caller, request));
    }

    @Operation(summary = "删除公告（管理员或教师）")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        AppPrincipal caller = CurrentUser.get();
        announcementService.delete(caller, id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "修改公告（管理员或教师）")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnnouncementResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateAnnouncementRequest request) {
        AppPrincipal caller = CurrentUser.get();
        return ResponseEntity.ok(announcementService.update(caller, id, request));
    }
}
