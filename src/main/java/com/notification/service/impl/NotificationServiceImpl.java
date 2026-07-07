package com.notification.service.impl;

import com.notification.dto.NotificationRequestDto;
import com.notification.dto.NotificationResponseDto;
import com.notification.entity.Notification;
import com.notification.enums.NotificationStatus;
import com.notification.exception.BusinessException;
import com.notification.repository.NotificationRepository;
import com.notification.service.NotificationService;
import com.notification.util.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

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

        // Map Entity to DTO and return
        return notificationMapper.toResponseDto(savedNotification);
    }
}
