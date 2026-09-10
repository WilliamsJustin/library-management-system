package com.school.library.service.impl;

import com.school.library.dto.AnnouncementResponse;
import com.school.library.dto.CreateAnnouncementRequest;
import com.school.library.entity.Announcement;
import com.school.library.entity.UserRole;
import com.school.library.exception.BusinessException;
import com.school.library.exception.ErrorCodes;
import com.school.library.exception.NotFoundException;
import com.school.library.repository.AnnouncementRepository;
import com.school.library.security.AppPrincipal;
import com.school.library.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementRepository repository;

    @Override
    public Page<AnnouncementResponse> list(Pageable pageable) {
        return repository.findLatest(pageable).map(AnnouncementResponse::fromEntity);
    }

    @Override
    @Transactional
    public AnnouncementResponse create(AppPrincipal caller, CreateAnnouncementRequest request) {
        requireManager(caller);
        Announcement announcement = new Announcement(
                request.getTitle(), request.getContent(), request.isPinned());
        return AnnouncementResponse.fromEntity(repository.save(announcement));
    }

    @Override
    @Transactional
    public void delete(AppPrincipal caller, Long id) {
        requireManager(caller);
        if (!repository.existsById(id)) {
            throw new NotFoundException("公告不存在");
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public AnnouncementResponse update(AppPrincipal caller, Long id, CreateAnnouncementRequest request) {
        requireManager(caller);
        Announcement announcement = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("公告不存在"));
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setPinned(request.isPinned());
        return AnnouncementResponse.fromEntity(repository.save(announcement));
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
