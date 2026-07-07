package com.notification.service;

import com.notification.dto.NotificationRequestDto;
import com.notification.dto.NotificationResponseDto;

import com.notification.enums.NotificationStatus;
import com.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.notification.dto.DashboardResponseDto;
import java.util.UUID;

public interface NotificationService {
    NotificationResponseDto createNotification(NotificationRequestDto requestDto);
    Page<NotificationResponseDto> getNotifications(NotificationStatus status, NotificationType type, Pageable pageable);
    NotificationResponseDto retryNotification(UUID id);
    DashboardResponseDto getDashboardStats();
}
