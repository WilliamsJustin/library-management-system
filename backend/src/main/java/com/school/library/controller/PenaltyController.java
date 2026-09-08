package com.school.library.controller;

import com.school.library.dto.PenaltyResponse;
import com.school.library.entity.PenaltyStatus;
import com.school.library.security.AppPrincipal;
import com.school.library.security.CurrentUser;
import com.school.library.service.PenaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/penalties")
@Tag(name = "逾期罚款", description = "罚款查询与缴费")
public class PenaltyController {

    @Autowired
    private PenaltyService penaltyService;

    @Operation(summary = "罚款查询（管理员）")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PenaltyResponse>> getPenalties(
            @Parameter(description = "状态") @RequestParam(required = false) PenaltyStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(penaltyService.getPenalties(status, pageable));
    }

    @Operation(summary = "我的罚款")
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<Page<PenaltyResponse>> getMyPenalties(Pageable pageable) {
        AppPrincipal caller = CurrentUser.get();
        return ResponseEntity.ok(penaltyService.getMyPenalties(caller, pageable));
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
