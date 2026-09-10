package com.school.library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.library.common.PageResult;
import com.school.library.common.Pages;
import com.school.library.dto.AnnouncementResponse;
import com.school.library.dto.CreateAnnouncementRequest;
import com.school.library.entity.Announcement;
import com.school.library.entity.UserRole;
import com.school.library.exception.BusinessException;
import com.school.library.exception.ErrorCodes;
import com.school.library.exception.NotFoundException;
import com.school.library.mapper.AnnouncementMapper;
import com.school.library.security.AppPrincipal;
import com.school.library.service.AnnouncementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    public AnnouncementServiceImpl(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    @Override
    public PageResult<AnnouncementResponse> list(int page, int size) {
        return list(null, null, null, page, size);
    }

    @Override
    public PageResult<AnnouncementResponse> list(String keyword, LocalDate startDate, LocalDate endDate,
                                                 int page, int size) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        // 开始日期取当天 00:00:00，结束日期取当天 23:59:59.999999999，保证按"日期"筛选语义正确
        LocalDateTime start = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime end = endDate == null ? null : endDate.atTime(LocalTime.MAX);

        LambdaQueryWrapper<Announcement> wrapper = Wrappers.<Announcement>lambdaQuery()
                .like(kw != null, Announcement::getTitle, kw)
                .ge(start != null, Announcement::getPublishedAt, start)
                .le(end != null, Announcement::getPublishedAt, end)
                // 置顶优先，其次按发布时间倒序
                .orderByDesc(Announcement::isPinned)
                .orderByDesc(Announcement::getPublishedAt);

        return PageResult.of(announcementMapper.selectPage(Pages.of(page, size), wrapper),
                AnnouncementResponse::fromEntity);
    }

    @Override
    @Transactional
    public AnnouncementResponse create(AppPrincipal caller, CreateAnnouncementRequest request) {
        requireManager(caller);
        Announcement announcement = new Announcement(
                request.getTitle(), request.getContent(), request.isPinned());
        announcementMapper.insert(announcement);
        return AnnouncementResponse.fromEntity(announcement);
    }

    @Override
    @Transactional
    public void delete(AppPrincipal caller, Long id) {
        requireManager(caller);
        if (announcementMapper.selectById(id) == null) {
            throw new NotFoundException("公告不存在");
        }
        announcementMapper.deleteById(id);
    }

    @Override
    @Transactional
    public AnnouncementResponse update(AppPrincipal caller, Long id, CreateAnnouncementRequest request) {
        requireManager(caller);
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new NotFoundException("公告不存在");
        }
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setPinned(request.isPinned());
        announcementMapper.updateById(announcement);
        return AnnouncementResponse.fromEntity(announcement);
    }

    /**
     * 公告管理权限：管理员（ADMIN 角色）或教师（READER 角色且 readerType=TEACHER）。
     * 注意：本系统教师不是独立角色，而是 READER 角色下的 readerType，故需在代码内判断。
     */
    private void requireManager(AppPrincipal caller) {
        boolean admin = caller.role() == UserRole.ADMIN;
        boolean teacher = caller.role() == UserRole.READER && "TEACHER".equals(caller.readerType());
        if (!admin && !teacher) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "只有管理员或教师可以管理公告");
        }
    }
}
