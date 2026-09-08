package com.school.library.controller;

import com.school.library.dto.AnnouncementResponse;
import com.school.library.entity.Announcement;
import com.school.library.repository.AnnouncementRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@Tag(name = "公共公告", description = "首页公告展示（匿名可访问）")
public class AnnouncementController {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Operation(summary = "最新公告列表")
    @GetMapping
    public ResponseEntity<List<AnnouncementResponse>> list(
            @RequestParam(defaultValue = "5") int limit) {
        int size = Math.min(Math.max(limit, 1), 20);
        List<AnnouncementResponse> list = announcementRepository.findLatest(size)
                .stream()
                .map(AnnouncementResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }
}
