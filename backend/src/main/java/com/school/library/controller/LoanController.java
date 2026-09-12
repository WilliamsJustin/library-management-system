package com.school.library.controller;

import com.school.library.common.PageResult;
import com.school.library.dto.BorrowRequest;
import com.school.library.dto.LoanResponse;
import com.school.library.entity.LoanStatus;
import com.school.library.entity.ReaderType;
import com.school.library.security.AppPrincipal;
import com.school.library.security.CurrentUser;
import com.school.library.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/loans")
@Tag(name = "借阅流通", description = "借出、归还、续借与借阅查询")
public class LoanController {

    @Autowired
    private LoanService loanService;

    @Operation(summary = "借出图书")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanResponse> borrow(@Valid @RequestBody BorrowRequest request) {
        return ResponseEntity.ok(loanService.borrow(request));
    }

    @Operation(summary = "读者自助借阅（借阅人为当前登录读者本人）")
    @PostMapping("/self")
    @PreAuthorize("hasRole('READER')")
    public ResponseEntity<LoanResponse> borrowSelf(@Valid @RequestBody BorrowRequest request) {
        AppPrincipal caller = CurrentUser.get();
        return ResponseEntity.ok(loanService.borrowSelf(caller, request));
    }

    @Operation(summary = "归还图书")
    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanResponse> returnLoan(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.returnLoan(id));
    }

    @Operation(summary = "读者自助还书（仅限归还本人借阅的图书）")
    @PostMapping("/{id}/self-return")
    @PreAuthorize("hasRole('READER')")
    public ResponseEntity<LoanResponse> returnSelf(@PathVariable Long id) {
        AppPrincipal caller = CurrentUser.get();
        return ResponseEntity.ok(loanService.returnSelf(caller, id));
    }

    @Operation(summary = "续借（读者可续借自己的图书，最多1次）")
    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<LoanResponse> renewLoan(@PathVariable Long id) {
        AppPrincipal caller = CurrentUser.get();
        return ResponseEntity.ok(loanService.renewLoan(id, caller));
    }

    @Operation(summary = "借阅查询（管理员）")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResult<LoanResponse>> getLoans(
            @Parameter(description = "读者ID") @RequestParam(required = false) Long readerId,
            @Parameter(description = "状态") @RequestParam(required = false) LoanStatus status,
            @Parameter(description = "读者类型：STUDENT 学生 / TEACHER 教师") @RequestParam(required = false) ReaderType readerType,
            @Parameter(description = "关键词，模糊匹配账号/学号/姓名/ISBN/书名") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(loanService.getLoans(readerId, status, readerType, keyword, page, size));
    }

    @Operation(summary = "我的借阅（含历史）")
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<PageResult<LoanResponse>> getMyLoans(
            @Parameter(description = "状态") @RequestParam(required = false) LoanStatus status,
            @Parameter(description = "关键词，模糊匹配ISBN/书名/条形码") @RequestParam(required = false) String keyword,
            @Parameter(description = "时间字段：BORROWED 借出（默认）/ DUE 应还 / RETURNED 归还")
            @RequestParam(required = false) String dateField,
            @Parameter(description = "起始日期（含当天）") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期（含当天）") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        AppPrincipal caller = CurrentUser.get();
        return ResponseEntity.ok(loanService.getMyLoans(caller, status, keyword,
                dateField, startDate, endDate, page, size));
    }
}
