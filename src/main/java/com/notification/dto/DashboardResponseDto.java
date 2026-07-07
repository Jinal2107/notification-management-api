package com.notification.dto;

import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponseDto {
    private Long totalNotifications;
    private Long sentCount;
    private Long failedCount;
    private Long retryCount;
    private Map<String, Long> typeWiseStatistics;
}
