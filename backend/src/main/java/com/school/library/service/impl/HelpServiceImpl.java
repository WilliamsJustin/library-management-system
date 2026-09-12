package com.school.library.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.library.common.PageResult;
import com.school.library.common.Pages;
import com.school.library.dto.*;
import com.school.library.entity.ChatMessage;
import com.school.library.entity.Faq;
import com.school.library.entity.FeedbackMessage;
import com.school.library.entity.Reader;
import com.school.library.entity.UserRole;
import com.school.library.exception.BusinessException;
import com.school.library.exception.ErrorCodes;
import com.school.library.exception.NotFoundException;
import com.school.library.mapper.ChatMessageMapper;
import com.school.library.mapper.FaqMapper;
import com.school.library.mapper.FeedbackMessageMapper;
import com.school.library.mapper.ReaderMapper;
import com.school.library.security.AppPrincipal;
import com.school.library.service.HelpService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HelpServiceImpl implements HelpService {

    private final FaqMapper faqMapper;
    private final FeedbackMessageMapper feedbackMapper;
    private final ChatMessageMapper chatMapper;
    private final ReaderMapper readerMapper;

    public HelpServiceImpl(FaqMapper faqMapper, FeedbackMessageMapper feedbackMapper,
                           ChatMessageMapper chatMapper, ReaderMapper readerMapper) {
        this.faqMapper = faqMapper;
        this.feedbackMapper = feedbackMapper;
        this.chatMapper = chatMapper;
        this.readerMapper = readerMapper;
    }

    /* ---------------- FAQ ---------------- */

    @Override
    public List<FaqResponse> searchFaq(String keyword) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Faq> list = faqMapper.selectList(Wrappers.<Faq>lambdaQuery()
                .eq(Faq::isEnabled, true)
                .orderByDesc(Faq::getUpdatedAt));
        // 条目量很小（几十条内），取出后在内存里做「问题或答案包含关键词」的模糊匹配
        return list.stream()
                .filter(f -> kw.isEmpty()
                        || f.getQuestion().toLowerCase().contains(kw)
                        || f.getAnswer().toLowerCase().contains(kw))
                .limit(20)
                .map(FaqResponse::fromEntity)
                .toList();
    }

    @Override
    public List<FaqResponse> listAllFaq() {
        return faqMapper.selectList(Wrappers.<Faq>lambdaQuery()
                        .orderByDesc(Faq::getUpdatedAt))
                .stream().map(FaqResponse::fromEntity).toList();
    }

    @Override
    @Transactional
    public FaqResponse createFaq(SaveFaqRequest request) {
        Faq faq = new Faq();
        applySave(faq, request);
        faqMapper.insert(faq);
        return FaqResponse.fromEntity(faq);
    }

    @Override
    @Transactional
    public FaqResponse updateFaq(Long id, SaveFaqRequest request) {
        Faq faq = faqMapper.selectById(id);
        if (faq == null) {
            throw new NotFoundException("FAQ 不存在");
        }
        applySave(faq, request);
        faq.setUpdatedAt(LocalDateTime.now());
        faqMapper.updateById(faq);
        return FaqResponse.fromEntity(faq);
    }

    @Override
    @Transactional
    public void deleteFaq(Long id) {
        if (faqMapper.deleteById(id) == 0) {
            throw new NotFoundException("FAQ 不存在");
        }
    }

    private void applySave(Faq faq, SaveFaqRequest request) {
        faq.setQuestion(request.question().trim());
        faq.setAnswer(request.answer().trim());
        faq.setEnabled(request.enabled() == null || request.enabled());
    }

    /* ---------------- 留言 ---------------- */

    @Override
    @Transactional
    public FeedbackResponse createFeedback(AppPrincipal caller, CreateFeedbackRequest request) {
        FeedbackMessage message = new FeedbackMessage();
        message.setContent(request.content().trim());
        if (caller != null) {
            // 登录读者 / 管理员：带上账号身份
            message.setReaderId(caller.userId());
            message.setReaderName(caller.account());
        } else {
            // 游客留言：昵称可填，缺省「游客」
            String name = request.contactName() == null || request.contactName().isBlank()
                    ? "游客" : request.contactName().trim();
            message.setReaderName(name);
        }
        feedbackMapper.insert(message);
        return FeedbackResponse.fromEntity(message);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FeedbackResponse> myFeedback(AppPrincipal caller, int page, int size) {
        IPage<FeedbackResponse> result = feedbackMapper.selectDetailPage(Pages.of(page, size),
                caller.userId(), null, null, null, null);
        return PageResult.of(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FeedbackResponse> listFeedback(String status, String keyword,
                                                     LocalDate startDate, LocalDate endDate,
                                                     int page, int size) {
        String st = status == null || status.isBlank() ? null : status.trim();
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        // 日期闭区间换算成 [start, endExclusive) 半开区间，保证「当天」完整包含
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endExclusive = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        IPage<FeedbackResponse> result = feedbackMapper.selectDetailPage(Pages.of(page, size),
                null, st, kw, start, endExclusive);
        return PageResult.of(result);
    }

    @Override
    @Transactional
    public FeedbackResponse replyFeedback(Long id, AppPrincipal caller, ReplyFeedbackRequest request) {
        FeedbackMessage message = feedbackMapper.selectById(id);
        if (message == null) {
            throw new NotFoundException("留言不存在");
        }
        message.setReplyContent(request.content().trim());
        message.setStatus(FeedbackMessage.STATUS_REPLIED);
        message.setRepliedAt(LocalDateTime.now());
        message.setRepliedBy(caller.account());
        feedbackMapper.updateById(message);
        return FeedbackResponse.fromEntity(message);
    }

    /* ---------------- 实时对话 ---------------- */

    @Override
    @Transactional
    public ChatMessageResponse sendChat(AppPrincipal caller, SendChatRequest request) {
        ChatMessage message = new ChatMessage();
        message.setContent(request.content().trim());
        if (caller.role() == UserRole.ADMIN) {
            if (request.readerId() == null) {
                throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "请指定会话读者（readerId）");
            }
            message.setReaderId(request.readerId());
            message.setSenderRole(ChatMessage.ROLE_ADMIN);
            message.setSenderName(caller.account());
        } else {
            message.setReaderId(caller.userId());
            message.setSenderRole(ChatMessage.ROLE_READER);
            message.setSenderName(caller.account());
        }
        chatMapper.insert(message);
        return ChatMessageResponse.fromEntity(message);
    }

    @Override
    public List<ChatMessageResponse> myChat(AppPrincipal caller) {
        return loadConversation(caller.userId());
    }

    @Override
    public List<ChatMessageResponse> chatWith(Long readerId) {
        return loadConversation(readerId);
    }

    private List<ChatMessageResponse> loadConversation(Long readerId) {
        // 会话消息量很小，直接倒序取最近 200 条后反转为正序
        List<ChatMessage> list = chatMapper.selectList(Wrappers.<ChatMessage>lambdaQuery()
                .eq(ChatMessage::getReaderId, readerId)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT 200"));
        return list.stream()
                .sorted(Comparator.comparing(ChatMessage::getId))
                .map(ChatMessageResponse::fromEntity)
                .toList();
    }

    @Override
    public List<ChatSessionResponse> chatSessions() {
        List<ChatMessage> all = chatMapper.selectList(Wrappers.<ChatMessage>lambdaQuery()
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT 2000"));
        if (all.isEmpty()) {
            return List.of();
        }
        // 按 readerId 聚合（LinkedHashMap 保持最近消息的顺序），组内消息同为 id 倒序
        Map<Long, List<ChatMessage>> byReader = new LinkedHashMap<>();
        for (ChatMessage m : all) {
            byReader.computeIfAbsent(m.getReaderId(), k -> new ArrayList<>()).add(m);
        }
        return byReader.entrySet().stream()
                .map(e -> {
                    List<ChatMessage> msgs = e.getValue();
                    ChatMessage last = msgs.get(0);
                    // 未回复数：从最新一条往前数，直到遇到管理员的回复为止的读者消息条数
                    long unreplied = 0;
                    for (ChatMessage m : msgs) {
                        if (ChatMessage.ROLE_ADMIN.equals(m.getSenderRole())) break;
                        unreplied++;
                    }
                    return new ChatSessionResponse(
                            e.getKey(),
                            readerDisplayName(e.getKey()),
                            last.getContent(),
                            last.getSenderRole(),
                            last.getCreatedAt(),
                            msgs.size(),
                            unreplied);
                })
                .toList();
    }

    private String readerDisplayName(Long readerId) {
        Reader reader = readerMapper.selectById(readerId);
        return reader != null ? reader.getName() + "（" + reader.getAccount() + "）" : "读者#" + readerId;
    }
}
