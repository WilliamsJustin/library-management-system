package com.school.library.repository;

import com.school.library.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** 置顶优先，其次按发布时间倒序，取最新 limit 条 */
    @Query("""
            SELECT a FROM Announcement a
            ORDER BY a.pinned DESC, a.publishedAt DESC
            """)
    List<Announcement> findLatest(int limit);
}
