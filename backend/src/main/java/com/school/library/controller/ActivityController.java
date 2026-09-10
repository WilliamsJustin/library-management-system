package com.school.library.controller;

import com.school.library.common.PageResult;
import com.school.library.dto.ActivityResponse;
import com.school.library.dto.CreateActivityRequest;
import com.school.library.security.AppPrincipal;
import com.school.library.security.CurrentUser;
import com.school.library.service.ActivityService;
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
@RequestMapping("/api/activities")
@Tag(name = "读者活动", description = "前台读者活动展示（公开）与管理（管理员/教师）")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Operation(summary = "读者活动列表（公开，无需登录，分页；支持标题模糊与发布时间区间筛选）")
    @GetMapping
    public ResponseEntity<PageResult<ActivityResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        int pageSize = Math.min(Math.max(size, 1), 50);
        return ResponseEntity.ok(activityService.list(keyword, startDate, endDate, page, pageSize));
    }

    @Operation(summary = "新增读者活动（管理员或教师）")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ActivityResponse> create(@Valid @RequestBody CreateActivityRequest request) {
        AppPrincipal caller = CurrentUser.get();
        return ResponseEntity.ok(activityService.create(caller, request));
    }

    @Operation(summary = "删除读者活动（管理员或教师）")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        AppPrincipal caller = CurrentUser.get();
        activityService.delete(caller, id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "修改读者活动（管理员或教师）")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ActivityResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateActivityRequest request) {
        AppPrincipal caller = CurrentUser.get();
        return ResponseEntity.ok(activityService.update(caller, id, request));
    }
}
