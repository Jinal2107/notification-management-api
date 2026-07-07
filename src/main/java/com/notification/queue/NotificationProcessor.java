package com.notification.queue;

import com.notification.entity.Notification;
import com.notification.enums.NotificationStatus;
import com.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessor {

    private final ExecutorService notificationExecutorService;
    private final NotificationRepository notificationRepository;
    private final Random random = new Random();

    public void processNotificationAsync(Notification notification) {
        UUID notificationId = notification.getId();
        log.info("Submitting notification {} for asynchronous processing", notificationId);

        notificationExecutorService.submit(() -> {
            try {
                process(notificationId);
            } catch (Exception e) {
                log.error("Failed to process notification {}", notificationId, e);
            }
        });
    }

    private void process(UUID notificationId) {
        log.info("Processing notification {}", notificationId);
        
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            log.warn("Notification {} not found in database", notificationId);
            return;
        }

        // Simulate random transmission success (70% SENT, 30% FAILED)
        boolean success = random.nextDouble() < 0.70;

        if (success) {
            notification.setStatus(NotificationStatus.SENT);
            log.info("Notification {} sent successfully", notificationId);
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            log.warn("Notification {} delivery failed", notificationId);
        }

        notificationRepository.save(notification);
    }
}
