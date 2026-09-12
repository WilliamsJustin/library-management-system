package com.school.library.controller;

import com.school.library.common.PageResult;
import com.school.library.dto.*;
import com.school.library.security.AppPrincipal;
import com.school.library.security.CurrentUser;
import com.school.library.service.HelpService;
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
import java.util.List;

/**
 * 帮助与反馈（FAQ 检索 / 留言板 / 实时对话）。
 *
 * <p>权限设计：FAQ 检索与提交留言公开（前台悬浮窗未登录也能用）；
 * 「我的留言」「我的会话」需要登录；FAQ 管理与留言回复仅管理员。
 */
@RestController
@RequestMapping("/api/help")
@Tag(name = "帮助与反馈", description = "FAQ 检索与管理、留言板、在线咨询对话")
public class HelpController {

    @Autowired
    private HelpService helpService;

    /* ---------------- FAQ ---------------- */

    @Operation(summary = "FAQ 关键词检索（公开；keyword 匹配问题或答案，为空返回前 20 条）")
    @GetMapping("/faq")
    public ResponseEntity<List<FaqResponse>> searchFaq(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(helpService.searchFaq(keyword));
    }

    @Operation(summary = "FAQ 全量列表（管理员，含停用条目）")
    @GetMapping("/faq/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FaqResponse>> listAllFaq() {
        return ResponseEntity.ok(helpService.listAllFaq());
    }

    @Operation(summary = "新增 FAQ（管理员）")
    @PostMapping("/faq")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FaqResponse> createFaq(@Valid @RequestBody SaveFaqRequest request) {
        return ResponseEntity.ok(helpService.createFaq(request));
    }

    @Operation(summary = "编辑 FAQ（管理员）")
    @PutMapping("/faq/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FaqResponse> updateFaq(@PathVariable Long id,
                                                 @Valid @RequestBody SaveFaqRequest request) {
        return ResponseEntity.ok(helpService.updateFaq(id, request));
    }

    @Operation(summary = "删除 FAQ（管理员）")
    @DeleteMapping("/faq/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFaq(@PathVariable Long id) {
        helpService.deleteFaq(id);
        return ResponseEntity.ok().build();
    }

    /* ---------------- 留言 ---------------- */

    @Operation(summary = "提交留言（公开；登录读者自动带账号名，游客可用昵称）")
    @PostMapping("/feedback")
    public ResponseEntity<FeedbackResponse> createFeedback(@Valid @RequestBody CreateFeedbackRequest request) {
        AppPrincipal caller = CurrentUser.getOrNull();
        return ResponseEntity.ok(helpService.createFeedback(caller, request));
    }

    @Operation(summary = "我的留言（登录读者）")
    @GetMapping("/feedback/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResult<FeedbackResponse>> myFeedback(
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(helpService.myFeedback(CurrentUser.get(), page, size));
    }

    @Operation(summary = "留言管理列表（管理员，可按状态筛选、关键词与留言时间区间检索）")
    @GetMapping("/feedback")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResult<FeedbackResponse>> listFeedback(
            @Parameter(description = "状态：UNREPLIED 未回复 / REPLIED 已回复")
            @RequestParam(required = false) String status,
            @Parameter(description = "关键词，模糊匹配账号/学号/留言人")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "留言起始日期（含当天）") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "留言结束日期（含当天）") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(helpService.listFeedback(status, keyword, startDate, endDate, page, size));
    }

    @Operation(summary = "回复留言（管理员）")
    @PostMapping("/feedback/{id}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeedbackResponse> replyFeedback(@PathVariable Long id,
                                                          @Valid @RequestBody ReplyFeedbackRequest request) {
        return ResponseEntity.ok(helpService.replyFeedback(id, CurrentUser.get(), request));
    }

    /* ---------------- 实时对话 ---------------- */

    @Operation(summary = "发送对话消息（读者发给自己会话；管理员需指定 readerId）")
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatMessageResponse> sendChat(@Valid @RequestBody SendChatRequest request) {
        return ResponseEntity.ok(helpService.sendChat(CurrentUser.get(), request));
    }

    @Operation(summary = "我的会话消息（读者轮询用，正序最近 200 条）")
    @GetMapping("/chat/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatMessageResponse>> myChat() {
        return ResponseEntity.ok(helpService.myChat(CurrentUser.get()));
    }

    @Operation(summary = "指定读者的会话消息（管理员轮询用）")
    @GetMapping("/chat")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ChatMessageResponse>> chatWith(
            @Parameter(description = "读者ID") @RequestParam Long readerId) {
        return ResponseEntity.ok(helpService.chatWith(readerId));
    }

    @Operation(summary = "会话列表（管理员，按读者聚合）")
    @GetMapping("/chat/sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ChatSessionResponse>> chatSessions() {
        return ResponseEntity.ok(helpService.chatSessions());
    }
}
