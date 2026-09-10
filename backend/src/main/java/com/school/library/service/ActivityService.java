package com.school.library.service;

import com.school.library.common.PageResult;
import com.school.library.dto.ActivityResponse;
import com.school.library.dto.CreateActivityRequest;
import com.school.library.security.AppPrincipal;

import java.time.LocalDate;

public interface ActivityService {

    /** 读者活动列表（公开，分页），page 为 0 基 */
    PageResult<ActivityResponse> list(int page, int size);

    /** 按标题模糊 + 发布时间区间筛选活动列表（公开，分页；条件为 null 表示不限制） */
    PageResult<ActivityResponse> list(String keyword, LocalDate startDate, LocalDate endDate, int page, int size);

    /** 新增活动（仅管理员或教师） */
    ActivityResponse create(AppPrincipal caller, CreateActivityRequest request);

    /** 删除活动（仅管理员或教师） */
    void delete(AppPrincipal caller, Long id);

    /** 修改活动（仅管理员或教师） */
    ActivityResponse update(AppPrincipal caller, Long id, CreateActivityRequest request);
}
