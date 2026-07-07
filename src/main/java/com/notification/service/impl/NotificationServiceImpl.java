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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

import com.notification.exception.ResourceNotFoundException;
import com.notification.dto.DashboardResponseDto;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationProcessor notificationProcessor;

    @Override
    @Transactional
    public NotificationResponseDto createNotification(NotificationRequestDto requestDto) {
        log.info("Attempting to create notification for userId: {}, type: {}", requestDto.getUserId(), requestDto.getType());
        
        // Business Rule: Same user cannot create same notification type and same message within 5 minutes
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        boolean isDuplicate = notificationRepository.existsByUserIdAndTypeAndMessageAndCreatedAtAfter(
                requestDto.getUserId(),
                requestDto.getType(),
                requestDto.getMessage(),
                fiveMinutesAgo
        );

        if (isDuplicate) {
            log.warn("Duplicate notification rejected for userId: {}, type: {} within 5 minutes", 
                    requestDto.getUserId(), requestDto.getType());
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
        log.info("Saved notification in database with ID: {}", savedNotification.getId());

        // Submit background task for asynchronous processing after transaction commit
        triggerAsyncProcessingPostCommit(savedNotification);

        // Map Entity to DTO and return
        return notificationMapper.toResponseDto(savedNotification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(NotificationStatus status, NotificationType type, Pageable pageable) {
        log.info("Fetching paginated notifications with status filter: {}, type filter: {}", status, type);
        Page<Notification> notificationsPage = notificationRepository.findAllByFilters(status, type, pageable);
        return notificationsPage.map(notificationMapper::toResponseDto);
    }

    @Override
    @Transactional
    public NotificationResponseDto retryNotification(UUID id) {
        log.info("Initiating retry process for notification ID: {}", id);
        
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Retry failed: Notification ID: {} not found", id);
                    return new ResourceNotFoundException("Notification not found with id: " + id);
                });

        // Rule 1: Status must be FAILED
        if (notification.getStatus() != NotificationStatus.FAILED) {
            log.warn("Retry rejected: Notification ID: {} is in {} status (must be FAILED)", id, notification.getStatus());
            throw new BusinessException("Notification status must be FAILED to retry.");
        }

        // Rule 2: Retry count less than 3
        if (notification.getRetryCount() >= 3) {
            log.warn("Retry rejected: Notification ID: {} has reached maximum retries (count: {})", id, notification.getRetryCount());
            throw new BusinessException("Notification has reached the maximum retry limit of 3.");
        }

        // Rule 3: Last retry must be older than 2 minutes (if last retry exists)
        boolean isRetryTooSoon = Optional.ofNullable(notification.getLastRetryTime())
                .map(lastRetry -> lastRetry.isAfter(LocalDateTime.now().minusMinutes(2)))
                .orElse(false);
        if (isRetryTooSoon) {
            log.warn("Retry rejected: Notification ID: {} was retried less than 2 minutes ago (lastRetryTime: {})", 
                    id, notification.getLastRetryTime());
            throw new BusinessException("Last retry was less than 2 minutes ago.");
        }

        // Increment retry count, change status, update last retry time
        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setStatus(NotificationStatus.RETRYING);
        notification.setLastRetryTime(LocalDateTime.now());

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Updated notification status to RETRYING and incremented retry count to {} for ID: {}", 
                savedNotification.getRetryCount(), id);

        // Submit task again to ExecutorService after transaction commits
        triggerAsyncProcessingPostCommit(savedNotification);

        return notificationMapper.toResponseDto(savedNotification);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponseDto getDashboardStats() {
        log.info("Gathering dashboard metrics from the repository");
        List<Object[]> statusCounts = notificationRepository.countGroupedByStatus();
        
        Map<NotificationStatus, Long> statusMap = statusCounts.stream()
                .collect(Collectors.toMap(
                        row -> (NotificationStatus) row[0],
                        row -> (Long) row[1]
                ));

        long totalNotifications = statusMap.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        long sentCount = statusMap.getOrDefault(NotificationStatus.SENT, 0L);
        long failedCount = statusMap.getOrDefault(NotificationStatus.FAILED, 0L);

        long totalRetries = notificationRepository.sumRetryCount();

        List<Object[]> typeCounts = notificationRepository.countGroupedByType();
        Map<String, Long> queryStats = typeCounts.stream()
                .collect(Collectors.toMap(
                        row -> ((NotificationType) row[0]).name(),
                        row -> (Long) row[1]
                ));

        Map<String, Long> typeWiseStats = Arrays.stream(NotificationType.values())
                .collect(Collectors.toMap(
                        NotificationType::name,
                        type -> queryStats.getOrDefault(type.name(), 0L)
                ));

        return DashboardResponseDto.builder()
                .totalNotifications(totalNotifications)
                .sentCount(sentCount)
                .failedCount(failedCount)
                .retryCount(totalRetries)
                .typeWiseStatistics(typeWiseStats)
                .build();
    }

    /**
     * Deduplicated helper to submit the notification for asynchronous processing only after the database transaction successfully commits.
     */
    private void triggerAsyncProcessingPostCommit(Notification notification) {
        log.debug("Registering post-commit transaction synchronization for notification ID: {}", notification.getId());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Transaction committed. Handing off notification ID: {} to the async processor.", notification.getId());
                notificationProcessor.processNotificationAsync(notification);
            }
        });
    }
}
