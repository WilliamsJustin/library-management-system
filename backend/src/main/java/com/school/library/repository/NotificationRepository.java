package com.school.library.repository;

import com.school.library.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByReaderIdOrderByCreatedAtDesc(Long readerId);

    long countByReaderIdAndReadFalse(Long readerId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.readerId = :readerId AND n.read = false")
    int markAllRead(@Param("readerId") Long readerId);
}
