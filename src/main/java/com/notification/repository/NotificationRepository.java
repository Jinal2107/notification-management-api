package com.notification.repository;

import com.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.notification.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    boolean existsByUserIdAndTypeAndMessageAndCreatedAtAfter(Long userId, NotificationType type, String message, LocalDateTime time);
}
