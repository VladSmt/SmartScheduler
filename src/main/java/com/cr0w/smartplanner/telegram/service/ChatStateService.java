package com.cr0w.smartplanner.telegram.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.cr0w.smartplanner.dto.CreateEventDTO;
import com.cr0w.smartplanner.telegram.enums.ChatState;
import com.cr0w.smartplanner.telegram.model.PendingAction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChatStateService {

    private static final Duration TTL = Duration.ofMinutes(15); // Трохи збільшив, щоб юзер встиг подумати

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper; // Додаємо для роботи з JSON

    public ChatStateService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }


    private String stateKey(long chatId) {
        return "chat:" + chatId + ":state";
    }

    private String dataKey(long chatId) {
        return "chat:" + chatId + ":data";
    }

    public void setState(long chatId, ChatState state) {
        redis.opsForValue().set(stateKey(chatId), state.name(), TTL);
    }

    public ChatState getState(long chatId) {
        String state = redis.opsForValue().get(stateKey(chatId));
        return state != null ? ChatState.valueOf(state) : ChatState.NONE;
    }

    public boolean hasState(long chatId) {
        return redis.hasKey(stateKey(chatId));
    }

    public void clearState(long chatId) {
        redis.delete(stateKey(chatId));
    }


    public void setTemporaryData(long chatId, CreateEventDTO data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redis.opsForValue().set(dataKey(chatId), json, TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DTO for chat {}: ", chatId, e);
        }
    }

    public CreateEventDTO getTemporaryData(long chatId) {
        String json = redis.opsForValue().get(dataKey(chatId));
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, CreateEventDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize DTO for chat {}: ", chatId, e);
            return null;
        }
    }

    public void setPendingAction(PendingAction action, Long chatId) {
        try {
            String json = objectMapper.writeValueAsString(action);
            redis.opsForValue().set(dataKey(chatId), json, TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PendingAction for chat {}: ", chatId, e);
        }
    }

    public PendingAction getPendingAction(long chatId) {
        String json = redis.opsForValue().get(dataKey(chatId));
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, PendingAction.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize PendingAction for chat {}: ", chatId, e);
            return null;
        }
    }

    public void clearPendingAction(long chatId) {
        redis.delete(dataKey(chatId));
    }

    public void clearTemporaryData(long chatId) {
        redis.delete(dataKey(chatId));
    }
}