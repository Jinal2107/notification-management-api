package com.notification.repository;

import com.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.notification.enums.NotificationType;
import com.notification.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.UUID;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    boolean existsByUserIdAndTypeAndMessageAndCreatedAtAfter(Long userId, NotificationType type, String message, LocalDateTime time);

    @Query("SELECT n FROM Notification n WHERE " +
           "(:status IS NULL OR n.status = :status) AND " +
           "(:type IS NULL OR n.type = :type)")
    Page<Notification> findAllByFilters(
            @Param("status") NotificationStatus status,
            @Param("type") NotificationType type,
            Pageable pageable
    );

    @Query("SELECT n.status, COUNT(n) FROM Notification n GROUP BY n.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT COALESCE(SUM(n.retryCount), 0) FROM Notification n")
    long sumRetryCount();

    @Query("SELECT n.type, COUNT(n) FROM Notification n GROUP BY n.type")
    List<Object[]> countGroupedByType();
}
