package com.cr0w.smartplanner.telegram.handlers;

import com.cr0w.smartplanner.dto.EventDTO;
import com.cr0w.smartplanner.exception.EventNotCreatedException;
import com.cr0w.smartplanner.exception.UserNotCreatedException;
import com.cr0w.smartplanner.service.EventService;
import com.cr0w.smartplanner.service.UserService;
import com.cr0w.smartplanner.telegram.ChatStateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Handler for multi-step manual event creation via Telegram.
 * Manages conversation state across 3 steps: title → description → date/time
 */
@RequiredArgsConstructor
@Component
public class ManualModeInputHandler implements BotCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(ManualModeInputHandler.class);

    private static final String STEP_1 = "MANUAL_MODE_STEP_1";
    private static final String STEP_2 = "MANUAL_MODE_STEP_2";
    private static final String STEP_3 = "MANUAL_MODE_STEP_3";
    private static final DateTimeFormatter DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Duration CACHE_TTL = Duration.ofMinutes(60);

    private final ChatStateService chatStateService;

    private final EventService eventService;

    private final StringRedisTemplate redis;

    @Override
    public boolean canHandle(Update update) {
        if (!update.hasMessage()) {
            return false;
        }

        long chatId = update.getMessage().getChatId();
        String state = chatStateService.getState(chatId);
        boolean result = (STEP_1.equals(state) || STEP_2.equals(state) || STEP_3.equals(state));

        logger.debug("ManualModeInputHandler.canHandle() for chat {} - state: {} - result: {}",
            chatId, state, result);
        return result;
    }

    @Override
    public void handle(Update update, TelegramClient client) {
        long chatId = update.getMessage().getChatId();
        String state = chatStateService.getState(chatId);
        String messageText = update.getMessage().getText();

        logger.info("ManualModeInputHandler processing state '{}' from chat: {}", state, chatId);

        try {
            if (STEP_1.equals(state)) {
                handleStep1(chatId, messageText, client);
            } else if (STEP_2.equals(state)) {
                handleStep2(chatId, messageText, client);
            } else if (STEP_3.equals(state)) {
                handleStep3(chatId, messageText, client);
            }
        } catch (TelegramApiException e) {
            logger.error("Telegram API error in ManualModeInputHandler for chat: {}: {}", chatId, e.getMessage(), e);
        }
    }

    /**
     * Step 1: Collect event title
     */
    private void handleStep1(long chatId, String title, TelegramClient client)
            throws TelegramApiException {
        logger.debug("Handling Step 1 (title collection) for chat: {} with title: '{}'", chatId, title);

        if (title == null || title.trim().isEmpty()) {
            logger.warn("Step 1 validation failed: empty title for chat: {}", chatId);
            sendMessage(chatId, "❌ Title can`t be empty:", client);
            return;
        }

        // Store title in Redis with TTL
        String trimmedTitle = title.trim();
        redis.opsForValue().set(getCacheKey(chatId, "title"), trimmedTitle, CACHE_TTL);
        logger.info("Title stored in cache for chat: {}", chatId);

        // Transition to Step 2
        chatStateService.setState(chatId, STEP_2);
        sendMessage(chatId, "✅ Title saved.\nStep 2: Description (input '-' for skip):", client);
        logger.info("Transitioned to STEP_2 for chat: {}", chatId);
    }

    /**
     * Step 2: Collect event description (optional)
     */
    private void handleStep2(long chatId, String description, TelegramClient client)
            throws TelegramApiException {
        logger.debug("Handling Step 2 (description collection) for chat: {}", chatId);
        checkSessionExpired(chatId, client);

        // Store description (null if "-")
        String cleanedDescription = null;
        if (description != null && !"-".equals(description.trim())) {
            cleanedDescription = description.trim();
            redis.opsForValue().set(getCacheKey(chatId, "description"), cleanedDescription, CACHE_TTL);
            logger.info("Description stored in cache for chat: {}", chatId);
        } else {
            logger.debug("Description skipped (dash) for chat: {}", chatId);
        }

        // Transition to Step 3
        chatStateService.setState(chatId, STEP_3);
        sendMessage(chatId, "✅ Description saved.\nStep 3: Date and Time (YYYY-MM-DD HH:mm) or send '-':", client);
        logger.info("Transitioned to STEP_3 for chat: {}", chatId);
    }

    /**
     * Step 3: Collect event date/time and create event
     */
    private void handleStep3(long chatId, String datetimeStr, TelegramClient client) throws TelegramApiException {
        logger.debug("Handling Step 3 (datetime and event creation) for chat: {}", chatId);
        if (checkSessionExpired(chatId, client)) return;

        // Retrieve title and description from cache
        String title = redis.opsForValue().get(getCacheKey(chatId, "title"));
        String description = redis.opsForValue().get(getCacheKey(chatId, "description"));

        // Parse datetime or set to null
        LocalDateTime eventDate;
        try{
            eventDate = parseDateTime(datetimeStr);
        } catch (DateTimeParseException e){
            logger.warn("Invalid date format provided for chat: {}", chatId);
            sendMessage(chatId, "Invalid date format. Please use 'YYYY-MM-DD HH:mm' or '-' to skip:", client);
            return;
        }

        // Create EventDTO (userId is NULL - service will fill it from tgId)
        EventDTO eventDTO = EventDTO.builder()
            .title(title)
            .description(description)              // Can be null
            .eventDate(eventDate)                  // Can be null
            .build();

        try {
            logger.info("Creating event for chat: {} with title: '{}' and description: {}",
                chatId, title, description != null ? "'" + description + "'" : "null");

            eventService.createEvent(eventDTO, chatId);

            sendMessage(chatId, "✅ Event created: " + title, client);
            logger.info("Event created successfully for chat: {}", chatId);
        } finally {
            cleanup(chatId);
        }
    }

    /**
     * Clear all temporary data and state for a chat session
     */
    private void cleanup(long chatId) {
        redis.delete(getCacheKey(chatId, "title"));
        redis.delete(getCacheKey(chatId, "description"));
        chatStateService.clearState(chatId);
        logger.debug("Cleaned up session for chat: {}", chatId);
    }

    /**
     * Send a Telegram message to user
     */
    private void sendMessage(long chatId, String text, TelegramClient client)
            throws TelegramApiException {
        SendMessage message = SendMessage.builder()
            .chatId(chatId)
            .text(text)
            .build();
        client.execute(message);
    }

    /**
     * Generate Redis cache key for a chat session
     */
    private String getCacheKey(long chatId, String field) {
        return "chat:" + chatId + ":temp:" + field;
    }

    private boolean checkSessionExpired(long chatId, TelegramClient client) throws TelegramApiException {
        if (redis.opsForValue().get(getCacheKey(chatId, "title")) == null) {
            cleanup(chatId);
            sendMessage(chatId, "Session expired, press /start", client);
            return true;
        }
        return false;
    }

    /**
     * Parse date/time string or return null if invalid
     */
    private LocalDateTime parseDateTime(String dateTimeStr){
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty() || "-".equals(dateTimeStr.trim())) return null;
        return LocalDateTime.parse(dateTimeStr.trim(), DATETIME_FORMATTER);
    }
}

