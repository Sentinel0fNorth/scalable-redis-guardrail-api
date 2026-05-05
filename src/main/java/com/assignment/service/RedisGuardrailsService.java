package com.assignment.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

@Service
public class RedisGuardrailsService {
    private static final Logger logger = LoggerFactory.getLogger(RedisGuardrailsService.class);

    private final RedisTemplate<String, String> redisTemplate;

    private static final int HORIZONTAL_CAP = 100;
    private static final int VERTICAL_CAP = 20;
    private static final long COOLDOWN_MINUTES = 10;
    private static final long NOTIFICATION_THROTTLE_MINUTES = 15;

    public RedisGuardrailsService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void incrementViralityScore(Long postId, String interactionType) {
        String key = "post:" + postId + ":virality_score";
        long points = 0;

        switch (interactionType) {
            case "BOT_REPLY":
                points = 1;
                break;
            case "HUMAN_LIKE":
                points = 20;
                break;
            case "HUMAN_COMMENT":
                points = 50;
                break;
            default:
                points = 0;
        }

        if (points > 0) {
            redisTemplate.opsForValue().increment(key, points);
        }
    }

    private static final String BOT_COUNT_SCRIPT = 
        "local current = tonumber(redis.call('GET', KEYS[1]) or '0')\n" +
        "local cap = tonumber(ARGV[1])\n" +
        "if current >= cap then\n" +
        "    return -1\n" +
        "else\n" +
        "    return redis.call('INCR', KEYS[1])\n" +
        "end";

    public boolean checkAndIncrementBotCount(Long postId) {
        String key = "post:" + postId + ":bot_count";
        
        org.springframework.data.redis.core.script.RedisScript<Long> script = 
            org.springframework.data.redis.core.script.RedisScript.of(BOT_COUNT_SCRIPT, Long.class);
            
        Long result = redisTemplate.execute(script, java.util.Collections.singletonList(key), String.valueOf(HORIZONTAL_CAP));

        if (result != null && result == -1L) {
            logger.warn("Horizontal cap exceeded for post: {}", postId);
            return false;
        }

        return true;
    }

    public boolean checkVerticalCap(Integer depthLevel) {
        if (depthLevel > VERTICAL_CAP) {
            logger.warn("Vertical cap exceeded. Depth level: {}", depthLevel);
            return false;
        }
        return true;
    }

    public boolean checkAndSetCooldown(Long botId, Long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;

        Boolean exists = redisTemplate.hasKey(key);
        if (exists != null && exists) {
            logger.warn("Cooldown active for bot: {} with human: {}", botId, humanId);
            return false;
        }

        redisTemplate.opsForValue().set(key, "1", COOLDOWN_MINUTES, TimeUnit.MINUTES);
        return true;
    }

    public boolean checkNotificationThrottle(Long userId) {
        String key = "notif_throttle:user_" + userId;

        Boolean exists = redisTemplate.hasKey(key);
        return exists == null || !exists;
    }

    public void setNotificationThrottle(Long userId) {
        String key = "notif_throttle:user_" + userId;
        redisTemplate.opsForValue().set(key, "1", NOTIFICATION_THROTTLE_MINUTES, TimeUnit.MINUTES);
    }

    public void addPendingNotification(Long userId, String notification) {
        String key = "user:" + userId + ":pending_notifs";
        redisTemplate.opsForList().rightPush(key, notification);
    }

    public long getPendingNotificationCount(Long userId) {
        String key = "user:" + userId + ":pending_notifs";
        Long count = redisTemplate.opsForList().size(key);
        return count != null ? count : 0;
    }

    public java.util.List<String> getAllPendingNotifications(Long userId) {
        String key = "user:" + userId + ":pending_notifs";
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size == 0) {
            return java.util.Collections.emptyList();
        }

        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public void clearPendingNotifications(Long userId) {
        String key = "user:" + userId + ":pending_notifs";
        redisTemplate.delete(key);
    }

    public void incrementLikeCount(Long postId) {
        String key = "post:" + postId + ":likes";
        redisTemplate.opsForValue().increment(key);
    }

    public long getLikeCount(Long postId) {
        String key = "post:" + postId + ":likes";
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }

    public Long getBotCountForPost(Long postId) {
        String key = "post:" + postId + ":bot_count";
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }

    public Long getViralityScore(Long postId) {
        String key = "post:" + postId + ":virality_score";
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }

    public RedisTemplate<String, String> getRedisTemplate() {
        return redisTemplate;
    }
}
