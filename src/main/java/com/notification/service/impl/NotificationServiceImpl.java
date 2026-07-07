package com.notification.service.impl;

import com.notification.dto.NotificationRequestDto;
import com.notification.dto.NotificationResponseDto;
import com.notification.entity.Notification;
import com.notification.enums.NotificationStatus;
import com.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.notification.exception.BusinessException;
import com.notification.queue.NotificationProcessor;
import com.notification.repository.NotificationRepository;
import com.notification.service.NotificationService;
import com.notification.util.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

import com.notification.exception.ResourceNotFoundException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationProcessor notificationProcessor;

    @Override
    @Transactional
    public NotificationResponseDto createNotification(NotificationRequestDto requestDto) {
        // Business Rule: Same user cannot create same notification type and same message within 5 minutes
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        boolean isDuplicate = notificationRepository.existsByUserIdAndTypeAndMessageAndCreatedAtAfter(
                requestDto.getUserId(),
                requestDto.getType(),
                requestDto.getMessage(),
                fiveMinutesAgo
        );

        if (isDuplicate) {
            throw new BusinessException(
                    "Duplicate notification: Same user cannot create the same notification type and message within 5 minutes."
            );
        }

        // Map DTO to Entity
        Notification notification = notificationMapper.toEntity(requestDto);
        
        // Set default values for new notification
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRetryCount(0);

        // Save entity
        Notification savedNotification = notificationRepository.save(notification);

        // Submit background task for asynchronous processing after transaction commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationProcessor.processNotificationAsync(savedNotification);
            }
        });

        // Map Entity to DTO and return
        return notificationMapper.toResponseDto(savedNotification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(NotificationStatus status, NotificationType type, Pageable pageable) {
        Page<Notification> notificationsPage = notificationRepository.findAllByFilters(status, type, pageable);
        return notificationsPage.map(notificationMapper::toResponseDto);
    }

    @Override
    @Transactional
    public NotificationResponseDto retryNotification(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        // Rule 1: Status must be FAILED
        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new BusinessException("Notification status must be FAILED to retry.");
        }

        // Rule 2: Retry count less than 3
        if (notification.getRetryCount() >= 3) {
            throw new BusinessException("Notification has reached the maximum retry limit of 3.");
        }

        // Rule 3: Last retry must be older than 2 minutes (if last retry exists)
        if (notification.getLastRetryTime() != null && 
                notification.getLastRetryTime().isAfter(LocalDateTime.now().minusMinutes(2))) {
            throw new BusinessException("Last retry was less than 2 minutes ago.");
        }

        // Increment retry count, change status, update last retry time
        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setStatus(NotificationStatus.RETRYING);
        notification.setLastRetryTime(LocalDateTime.now());

        Notification savedNotification = notificationRepository.save(notification);

        // Submit task again to ExecutorService after transaction commits
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationProcessor.processNotificationAsync(savedNotification);
            }
        });

        return notificationMapper.toResponseDto(savedNotification);
    }
}
