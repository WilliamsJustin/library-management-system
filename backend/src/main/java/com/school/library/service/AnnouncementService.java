package com.school.library.service;

import com.school.library.common.PageResult;
import com.school.library.dto.AnnouncementResponse;
import com.school.library.dto.CreateAnnouncementRequest;
import com.school.library.security.AppPrincipal;

import java.time.LocalDate;

public interface AnnouncementService {

    /** 最新公告列表（公开，分页），page 为 0 基 */
    PageResult<AnnouncementResponse> list(int page, int size);

    /** 按标题模糊 + 发布时间区间筛选公告列表（公开，分页；条件为 null 表示不限制） */
    PageResult<AnnouncementResponse> list(String keyword, LocalDate startDate, LocalDate endDate, int page, int size);

    /** 发布新公告（仅管理员或教师可读） */
    AnnouncementResponse create(AppPrincipal caller, CreateAnnouncementRequest request);

    /** 删除公告（仅管理员或教师可读） */
    void delete(AppPrincipal caller, Long id);

    /** 修改公告（仅管理员或教师可读） */
    AnnouncementResponse update(AppPrincipal caller, Long id, CreateAnnouncementRequest request);
}
