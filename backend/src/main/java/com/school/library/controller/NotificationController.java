package com.school.library.controller;

import com.school.library.dto.NotificationResponse;
import com.school.library.entity.Notification;
import com.school.library.repository.NotificationRepository;
import com.school.library.security.AppPrincipal;
import com.school.library.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "站内消息", description = "逾期与到期提醒")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Operation(summary = "我的消息（最近50条）")
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<List<NotificationResponse>> myNotifications() {
        AppPrincipal caller = CurrentUser.get();
        List<NotificationResponse> list = notificationRepository
                .findTop50ByReaderIdOrderByCreatedAtDesc(caller.userId())
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "未读消息数")
    @GetMapping("/my/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        AppPrincipal caller = CurrentUser.get();
        return ResponseEntity.ok(Map.of("count",
                notificationRepository.countByReaderIdAndReadFalse(caller.userId())));
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/my/read-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'READER')")
    @Transactional
    public ResponseEntity<Void> markAllRead() {
        AppPrincipal caller = CurrentUser.get();
        notificationRepository.markAllRead(caller.userId());
        return ResponseEntity.ok().build();
    }
}
