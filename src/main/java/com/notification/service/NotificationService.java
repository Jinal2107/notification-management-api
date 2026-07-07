package com.notification.service;

import com.notification.dto.NotificationRequestDto;
import com.notification.dto.NotificationResponseDto;

public interface NotificationService {
    NotificationResponseDto createNotification(NotificationRequestDto requestDto);
}
