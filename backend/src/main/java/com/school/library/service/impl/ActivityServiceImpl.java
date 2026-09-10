package com.school.library.service.impl;

import com.school.library.dto.ActivityResponse;
import com.school.library.dto.CreateActivityRequest;
import com.school.library.entity.Activity;
import com.school.library.entity.UserRole;
import com.school.library.exception.BusinessException;
import com.school.library.exception.ErrorCodes;
import com.school.library.exception.NotFoundException;
import com.school.library.repository.ActivityRepository;
import com.school.library.security.AppPrincipal;
import com.school.library.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityServiceImpl implements ActivityService {

    @Autowired
    private ActivityRepository repository;

    @Override
    public Page<ActivityResponse> list(Pageable pageable) {
        return repository.findLatest(pageable).map(ActivityResponse::fromEntity);
    }

    @Override
    @Transactional
    public ActivityResponse create(AppPrincipal caller, CreateActivityRequest request) {
        requireManager(caller);
        Activity activity = new Activity(
                request.getTitle(), request.getContent(),
                request.getDateText(), request.getTag(), request.isPinned());
        return ActivityResponse.fromEntity(repository.save(activity));
    }

    @Override
    @Transactional
    public void delete(AppPrincipal caller, Long id) {
        requireManager(caller);
        if (!repository.existsById(id)) {
            throw new NotFoundException("活动不存在");
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public ActivityResponse update(AppPrincipal caller, Long id, CreateActivityRequest request) {
        requireManager(caller);
        Activity activity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("活动不存在"));
        activity.setTitle(request.getTitle());
        activity.setContent(request.getContent());
        activity.setDateText(request.getDateText());
        activity.setTag(request.getTag());
        activity.setPinned(request.isPinned());
        return ActivityResponse.fromEntity(repository.save(activity));
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
