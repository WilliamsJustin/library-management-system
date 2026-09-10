package com.school.library.service;

import com.school.library.dto.ActivityResponse;
import com.school.library.dto.CreateActivityRequest;
import com.school.library.security.AppPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ActivityService {

    /** 读者活动列表（公开，分页） */
    Page<ActivityResponse> list(Pageable pageable);

    /** 新增活动（仅管理员或教师） */
    ActivityResponse create(AppPrincipal caller, CreateActivityRequest request);

    /** 删除活动（仅管理员或教师） */
    void delete(AppPrincipal caller, Long id);

    /** 修改活动（仅管理员或教师） */
    ActivityResponse update(AppPrincipal caller, Long id, CreateActivityRequest request);
}
