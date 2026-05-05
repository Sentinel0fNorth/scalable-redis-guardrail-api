package com.assignment.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Set;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final RedisGuardrailsService redisGuardrailsService;

    public NotificationService(RedisGuardrailsService redisGuardrailsService) {
        this.redisGuardrailsService = redisGuardrailsService;
    }

    public void handleNotification(Long userId, String botName) {
        boolean shouldSendImmediate = redisGuardrailsService.checkNotificationThrottle(userId);

        if (shouldSendImmediate) {
            logger.info("Push Notification Sent to User {}: Bot {} replied to your post", userId, botName);
            redisGuardrailsService.setNotificationThrottle(userId);
        } else {
            String notification = "Bot " + botName + " replied to your post";
            redisGuardrailsService.addPendingNotification(userId, notification);
        }
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    public void sweepPendingNotifications() {
        logger.info("Starting notification sweep...");

        Set<String> keys = redisGuardrailsService.getRedisTemplate()
                .keys("user:*:pending_notifs");

        if (keys == null || keys.isEmpty()) {
            logger.info("No pending notifications to sweep");
            return;
        }

        for (String key : keys) {
            String userIdStr = key.split(":")[1];
            Long userId = Long.parseLong(userIdStr);

            List<String> notifications = redisGuardrailsService.getAllPendingNotifications(userId);

            if (notifications != null && !notifications.isEmpty()) {
                int count = notifications.size();
                String summary = "Summarized Push Notification: " + notifications.get(0);
                if (count > 1) {
                    summary += " and [" + (count - 1) + "] others interacted with your posts";
                }
                logger.info(summary);

                redisGuardrailsService.clearPendingNotifications(userId);
            }
        }
    }
}
