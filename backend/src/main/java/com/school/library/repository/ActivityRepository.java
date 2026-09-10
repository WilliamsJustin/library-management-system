package com.school.library.repository;

import com.school.library.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    /** 置顶优先，其次按创建时间倒序，分页返回 */
    @Query("""
            SELECT a FROM Activity a
            ORDER BY a.pinned DESC, a.createdAt DESC
            """)
    Page<Activity> findLatest(Pageable pageable);
}
