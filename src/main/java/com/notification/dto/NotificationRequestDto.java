package com.notification.dto;

import com.notification.enums.NotificationType;
import com.notification.validation.NotRepeatedWords;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequestDto {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotBlank(message = "Message is required")
    @NotRepeatedWords
    private String message;

    private LocalDateTime scheduleTime;
}
