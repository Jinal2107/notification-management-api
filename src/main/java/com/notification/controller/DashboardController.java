package com.notification.controller;

import com.notification.dto.DashboardResponseDto;
import com.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<DashboardResponseDto> getDashboardStats() {
        DashboardResponseDto stats = notificationService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
}
