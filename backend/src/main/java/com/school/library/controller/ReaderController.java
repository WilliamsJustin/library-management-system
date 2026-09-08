package com.school.library.controller;

import com.school.library.dto.*;
import com.school.library.entity.Reader;
import com.school.library.entity.ReaderStatus;
import com.school.library.entity.ReaderType;
import com.school.library.service.ReaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "查询读者列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<Page<ReaderResponse>> getReaders(
            @Parameter(description = "关键词（姓名/账号/学号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "读者类型") @RequestParam(required = false) ReaderType type,
            @Parameter(description = "状态：true 正常 / false 停借") @RequestParam(required = false) Boolean status,
            Pageable pageable) {
        ReaderStatus readerStatus = status == null ? null
                : (status ? ReaderStatus.NORMAL : ReaderStatus.RESTRICTED);
        Page<Reader> readers = readerService.getReaders(keyword, type, readerStatus, pageable);
        Page<ReaderResponse> response = readers.map(ReaderResponse::fromEntity);
        return ResponseEntity.ok(response);
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
