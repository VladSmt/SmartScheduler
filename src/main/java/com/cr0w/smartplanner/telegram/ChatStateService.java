package com.cr0w.smartplanner.telegram;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ChatStateService {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;

    public ChatStateService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(long chatId) {
        return "chat:" + chatId + ":state";
    }

    public void setState(long chatId, String state) {
        redis.opsForValue().set(key(chatId), state, TTL);
    }

    public String getState(long chatId) {
        return redis.opsForValue().get(key(chatId));
    }

    public void clearState(long chatId) {
        redis.delete(key(chatId));
    }

}
