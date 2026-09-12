package com.school.library.controller;

import com.school.library.dto.DashboardStatsResponse;
import com.school.library.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "统计数据", description = "管理员首页仪表盘概况")
public class StatsController {

    @Autowired
    private StatsService statsService;

    @Operation(summary = "首页概况（管理员）：借阅流通 / 逾期罚款 / 实时咨询")
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardStatsResponse> overview() {
        return ResponseEntity.ok(statsService.overview());
    }
}
