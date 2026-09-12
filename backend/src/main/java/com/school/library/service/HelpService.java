package com.school.library.service;

import com.school.library.common.PageResult;
import com.school.library.dto.*;
import com.school.library.security.AppPrincipal;

import java.time.LocalDate;
import java.util.List;

public interface HelpService {

    /* ---------------- FAQ ---------------- */

    /** FAQ 关键词检索（公开，仅启用的条目；keyword 为空返回前 20 条） */
    List<FaqResponse> searchFaq(String keyword);

    /** FAQ 全量列表（管理员，含停用条目） */
    List<FaqResponse> listAllFaq();

    FaqResponse createFaq(SaveFaqRequest request);

    FaqResponse updateFaq(Long id, SaveFaqRequest request);

    void deleteFaq(Long id);

    /* ---------------- 留言 ---------------- */

    /** 提交留言（登录读者自动带账号名，游客用昵称「游客」） */
    FeedbackResponse createFeedback(AppPrincipal caller, CreateFeedbackRequest request);

    /** 我的留言（读者） */
    PageResult<FeedbackResponse> myFeedback(AppPrincipal caller, int page, int size);

    /**
     * 留言管理（管理员），page 为 0 基。
     * keyword 模糊匹配账号/学号/留言人；startDate/endDate 为留言时间闭区间（均可为空）。
     */
    PageResult<FeedbackResponse> listFeedback(String status, String keyword,
                                              LocalDate startDate, LocalDate endDate,
                                              int page, int size);

    FeedbackResponse replyFeedback(Long id, AppPrincipal caller, ReplyFeedbackRequest request);

    /* ---------------- 实时对话 ---------------- */

    /** 发送对话消息：读者固定写入自己的会话；管理员必须指定 readerId */
    ChatMessageResponse sendChat(AppPrincipal caller, SendChatRequest request);

    /** 读者的会话消息（按时间正序，最多最近 200 条） */
    List<ChatMessageResponse> myChat(AppPrincipal caller);

    /** 管理员查看指定读者的会话（按时间正序，最多最近 200 条） */
    List<ChatMessageResponse> chatWith(Long readerId);

    /** 管理员的会话列表（按读者聚合，最近消息倒序） */
    List<ChatSessionResponse> chatSessions();
}
