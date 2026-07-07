package com.notification.dto;

import com.notification.enums.NotificationStatus;
import com.notification.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDto {
    private UUID id;
    private Long userId;
    private NotificationType type;
    private String message;
    private NotificationStatus status;
    private Integer retryCount;
    private LocalDateTime scheduleTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastRetryTime;
}
