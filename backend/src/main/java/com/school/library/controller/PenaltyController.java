package com.school.library.controller;

import com.school.library.common.PageResult;
import com.school.library.dto.PenaltyResponse;
import com.school.library.entity.PenaltyStatus;
import com.school.library.entity.ReaderType;
import com.school.library.security.AppPrincipal;
import com.school.library.security.CurrentUser;
import com.school.library.service.PenaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/penalties")
@Tag(name = "逾期罚款", description = "罚款查询与缴费")
public class PenaltyController {

    @Autowired
    private PenaltyService penaltyService;

    @Operation(summary = "罚款查询（管理员）")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResult<PenaltyResponse>> getPenalties(
            @Parameter(description = "状态") @RequestParam(required = false) PenaltyStatus status,
            @Parameter(description = "读者类型：STUDENT 学生 / TEACHER 教师") @RequestParam(required = false) ReaderType readerType,
            @Parameter(description = "关键词，模糊匹配账号/学号/姓名/ISBN/书名") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(penaltyService.getPenalties(status, readerType, keyword, page, size));
    }

    @Operation(summary = "我的罚款")
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<PageResult<PenaltyResponse>> getMyPenalties(
            @Parameter(description = "状态") @RequestParam(required = false) PenaltyStatus status,
            @Parameter(description = "关键词，模糊匹配ISBN/书名/条形码") @RequestParam(required = false) String keyword,
            @Parameter(description = "时间字段：CREATED 生成（默认）/ PAID 缴费")
            @RequestParam(required = false) String dateField,
            @Parameter(description = "起始日期（含当天）") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期（含当天）") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        AppPrincipal caller = CurrentUser.get();
        return ResponseEntity.ok(penaltyService.getMyPenalties(caller, status, keyword,
                dateField, startDate, endDate, page, size));
    }

    @Operation(summary = "缴纳罚款（缴清后自动恢复借阅资格）")
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<Void> payPenalty(@PathVariable Long id) {
        AppPrincipal caller = CurrentUser.get();
        penaltyService.payPenalty(id, caller);
        return ResponseEntity.ok().build();
    }
}
