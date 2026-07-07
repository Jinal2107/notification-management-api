package com.notification.controller;

import com.notification.dto.NotificationRequestDto;
import com.notification.dto.NotificationResponseDto;
import com.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.notification.enums.NotificationStatus;
import com.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponseDto> createNotification(@Valid @RequestBody NotificationRequestDto requestDto) {
        NotificationResponseDto responseDto = notificationService.createNotification(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponseDto>> getNotifications(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationType type,
            Pageable pageable) {
        Page<NotificationResponseDto> response = notificationService.getNotifications(status, type, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<NotificationResponseDto> retryNotification(@PathVariable UUID id) {
        NotificationResponseDto responseDto = notificationService.retryNotification(id);
        return ResponseEntity.ok(responseDto);
    }
}
