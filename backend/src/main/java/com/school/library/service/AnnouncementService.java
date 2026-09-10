package com.school.library.service;

import com.school.library.dto.AnnouncementResponse;
import com.school.library.dto.CreateAnnouncementRequest;
import com.school.library.security.AppPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnnouncementService {

    /** 最新公告列表（公开，分页） */
    Page<AnnouncementResponse> list(Pageable pageable);

    /** 发布新公告（仅管理员或教师可读） */
    AnnouncementResponse create(AppPrincipal caller, CreateAnnouncementRequest request);

    /** 删除公告（仅管理员或教师可读） */
    void delete(AppPrincipal caller, Long id);

    /** 修改公告（仅管理员或教师可读） */
    AnnouncementResponse update(AppPrincipal caller, Long id, CreateAnnouncementRequest request);
}
