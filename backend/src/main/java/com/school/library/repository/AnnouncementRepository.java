package com.school.library.repository;

import com.school.library.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** 置顶优先，其次按发布时间倒序，分页返回 */
    @Query("""
            SELECT a FROM Announcement a
            ORDER BY a.pinned DESC, a.publishedAt DESC
            """)
    Page<Announcement> findLatest(Pageable pageable);
}
