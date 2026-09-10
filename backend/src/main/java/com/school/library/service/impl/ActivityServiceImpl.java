package com.school.library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.library.common.PageResult;
import com.school.library.common.Pages;
import com.school.library.dto.ActivityResponse;
import com.school.library.dto.CreateActivityRequest;
import com.school.library.entity.Activity;
import com.school.library.entity.UserRole;
import com.school.library.exception.BusinessException;
import com.school.library.exception.ErrorCodes;
import com.school.library.exception.NotFoundException;
import com.school.library.mapper.ActivityMapper;
import com.school.library.security.AppPrincipal;
import com.school.library.service.ActivityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;

    public ActivityServiceImpl(ActivityMapper activityMapper) {
        this.activityMapper = activityMapper;
    }

    @Override
    public PageResult<ActivityResponse> list(int page, int size) {
        return list(null, null, null, page, size);
    }

    @Override
    public PageResult<ActivityResponse> list(String keyword, LocalDate startDate, LocalDate endDate,
                                             int page, int size) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        // 开始日期取当天 00:00:00，结束日期取当天 23:59:59.999999999，保证按"日期"筛选语义正确
        LocalDateTime start = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime end = endDate == null ? null : endDate.atTime(LocalTime.MAX);

        LambdaQueryWrapper<Activity> wrapper = Wrappers.<Activity>lambdaQuery()
                .like(kw != null, Activity::getTitle, kw)
                .ge(start != null, Activity::getCreatedAt, start)
                .le(end != null, Activity::getCreatedAt, end)
                // 置顶优先，其次按创建时间倒序
                .orderByDesc(Activity::isPinned)
                .orderByDesc(Activity::getCreatedAt);

        return PageResult.of(activityMapper.selectPage(Pages.of(page, size), wrapper),
                ActivityResponse::fromEntity);
    }

    @Override
    @Transactional
    public ActivityResponse create(AppPrincipal caller, CreateActivityRequest request) {
        requireManager(caller);
        // 发布时间由系统自动生成（createdAt），不接受前端传入
        Activity activity = new Activity(
                request.getTitle(), request.getContent(),
                request.getTag(), request.isPinned());
        activityMapper.insert(activity);
        return ActivityResponse.fromEntity(activity);
    }

    @Override
    @Transactional
    public void delete(AppPrincipal caller, Long id) {
        requireManager(caller);
        if (activityMapper.selectById(id) == null) {
            throw new NotFoundException("活动不存在");
        }
        activityMapper.deleteById(id);
    }

    @Override
    @Transactional
    public ActivityResponse update(AppPrincipal caller, Long id, CreateActivityRequest request) {
        requireManager(caller);
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new NotFoundException("活动不存在");
        }
        activity.setTitle(request.getTitle());
        activity.setContent(request.getContent());
        activity.setTag(request.getTag());
        activity.setPinned(request.isPinned());
        activityMapper.updateById(activity);
        return ActivityResponse.fromEntity(activity);
    }

    /**
     * 活动管理权限：管理员（ADMIN 角色）或教师（READER 角色且 readerType=TEACHER）。
     * 注意：本系统教师不是独立角色，而是 READER 角色下的 readerType，故需在代码内判断。
     */
    private void requireManager(AppPrincipal caller) {
        boolean admin = caller.role() == UserRole.ADMIN;
        boolean teacher = caller.role() == UserRole.READER && "TEACHER".equals(caller.readerType());
        if (!admin && !teacher) {
            throw new BusinessException(ErrorCodes.FORBIDDEN, "只有管理员或教师可以管理读者活动");
        }
    }
}
