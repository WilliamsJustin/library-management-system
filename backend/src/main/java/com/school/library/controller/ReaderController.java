package com.school.library.controller;

import com.school.library.common.PageResult;
import com.school.library.dto.*;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderStatus;
import com.school.library.entity.ReaderType;
import com.school.library.service.ReaderService;
import com.school.library.service.impl.ReaderServiceImpl;
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
import java.util.Map;

@RestController
@RequestMapping("/api/readers")
@Tag(name = "读者管理", description = "读者信息管理接口")
public class ReaderController {

    @Autowired
    private ReaderService readerService;

    @Operation(summary = "创建读者")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReaderResponse> createReader(@Valid @RequestBody CreateReaderRequest request) {
        Reader reader = readerService.createReader(request);
        return ResponseEntity.ok(ReaderResponse.fromEntity(reader));
    }

    @Operation(summary = "更新读者信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReaderResponse> updateReader(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReaderRequest request) {
        Reader reader = readerService.updateReader(id, request);
        return ResponseEntity.ok(ReaderResponse.fromEntity(reader));
    }

    @Operation(summary = "恢复正常/停借")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleReaderStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest request) {
        readerService.toggleStatus(id, request.getStatus());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "符合筛选条件的全部读者 ID（跨页全选用，管理员）")
    @GetMapping("/ids")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Long>> listReaderIds(
            @Parameter(description = "关键词（姓名/账号/学号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "读者类型") @RequestParam(required = false) ReaderType type,
            @Parameter(description = "状态：true 正常 / false 停借") @RequestParam(required = false) Boolean status) {
        return ResponseEntity.ok(readerService.listReaderIds(keyword, type,
                status == null ? null : status ? ReaderStatus.NORMAL : ReaderStatus.RESTRICTED));
    }

    @Operation(summary = "重置密码（管理员）：恢复为初始密码 pass123，仅对读者账号生效")
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable Long id) {
        readerService.resetPassword(id);
        return ResponseEntity.ok(Map.of("password", ReaderService.DEFAULT_RESET_PASSWORD));
    }

    @Operation(summary = "批量重置密码（管理员），返回实际重置的条数")
    @PostMapping("/batch/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> batchResetPassword(
            @Valid @RequestBody BatchResetPasswordRequest request) {
        return ResponseEntity.ok(Map.of("count", readerService.batchResetPassword(request.ids())));
    }

    @Operation(summary = "批量设置状态（管理员）：true 正常 / false 停借，返回实际更新的条数")
    @PatchMapping("/batch/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> batchUpdateStatus(
            @Valid @RequestBody BatchUpdateStatusRequest request) {
        return ResponseEntity.ok(Map.of("count", readerService.batchUpdateStatus(request.ids(), request.status())));
    }

    @Operation(summary = "Excel 批量导入读者（管理员）：列顺序 账号|密码|姓名|类型|学号/工号|手机号")
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExcelImportResult> importReaders(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(readerService.importReaders(file));
    }

    @Operation(summary = "下载读者导入模板（管理员）")
    @GetMapping("/import/template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> importTemplate() {
        byte[] bytes = ExcelTemplateUtil.build(
                ReaderServiceImpl.EXPORT_HEADERS,
                List.of(
                        new String[]{"stu001", "pass123", "张三", "学生", "20240001", "13800000001"},
                        new String[]{"tea001", "pass123", "李老师", "教师", "T2001001", "13800000002"}
                ),
                ReaderServiceImpl.EXPORT_WIDTHS);
        return ExcelTemplateUtil.toResponse(bytes, ReaderServiceImpl.TEMPLATE_FILENAME);
    }

    @Operation(summary = "导出读者（管理员）：scope=all 全部 / page 单页 / selected 选中（ids 逗号分隔），文件与导入模板同款式，密码列恒为空")
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportReaders(
            @Parameter(description = "导出范围：all / page / selected") @RequestParam(defaultValue = "all") String scope,
            @Parameter(description = "关键词（姓名/账号/学号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "读者类型") @RequestParam(required = false) ReaderType type,
            @Parameter(description = "状态：true 正常 / false 停借") @RequestParam(required = false) Boolean status,
            @Parameter(description = "页码，scope=page 时使用") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数，scope=page 时使用") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "选中的读者 ID，逗号分隔，scope=selected 时使用") @RequestParam(required = false) String ids) {
        byte[] bytes = readerService.exportReaders(scope, keyword, type,
                status == null ? null : status ? ReaderStatus.NORMAL : ReaderStatus.RESTRICTED,
                page, size, parseIds(ids));
        return ExcelTemplateUtil.toResponse(bytes, ReaderServiceImpl.EXPORT_FILENAME);
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

    @Operation(summary = "查询读者列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<PageResult<ReaderResponse>> getReaders(
            @Parameter(description = "关键词（姓名/账号/学号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "读者类型") @RequestParam(required = false) ReaderType type,
            @Parameter(description = "状态：true 正常 / false 停借") @RequestParam(required = false) Boolean status,
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        ReaderStatus readerStatus = status == null ? null
                : (status ? ReaderStatus.NORMAL : ReaderStatus.RESTRICTED);
        return ResponseEntity.ok(readerService.getReaders(keyword, type, readerStatus, page, size));
    }

    @Operation(summary = "获取读者详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<ReaderResponse> getReader(@PathVariable Long id) {
        Reader reader = readerService.getReader(id);
        return ResponseEntity.ok(ReaderResponse.fromEntity(reader));
    }

    @Operation(summary = "删除读者")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReader(@PathVariable Long id) {
        readerService.deleteReader(id);
        return ResponseEntity.ok().build();
    }
}
