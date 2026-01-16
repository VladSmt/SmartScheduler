package com.cr0w.smartplanner.telegram.handlers;

import com.cr0w.smartplanner.dto.EventDTO;
import com.cr0w.smartplanner.exception.EventNotCreatedException;
import com.cr0w.smartplanner.exception.UserNotCreatedException;
import com.cr0w.smartplanner.service.EventService;
import com.cr0w.smartplanner.service.UserService;
import com.cr0w.smartplanner.telegram.ChatStateService;
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
@Component
public class ManualModeInputHandler implements BotCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(ManualModeInputHandler.class);

    private static final String STEP_1 = "MANUAL_MODE_STEP_1";
    private static final String STEP_2 = "MANUAL_MODE_STEP_2";
    private static final String STEP_3 = "MANUAL_MODE_STEP_3";
    private static final DateTimeFormatter DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Autowired
    private ChatStateService chatStateService;

    @Autowired
    private EventService eventService;

    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate redis;

    @Override
    public boolean canHandle(Update update) {
        if (!update.hasMessage()) {
            return false;
        }

        long chatId = update.getMessage().getChatId();
        String state = chatStateService.getState(chatId);
        boolean result = state != null && (STEP_1.equals(state) || STEP_2.equals(state) || STEP_3.equals(state));

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
            sendMessage(chatId, "❌ Назва не може бути пустою. Спробуй ще раз:", client);
            return;
        }

        // Store title in Redis with TTL
        String trimmedTitle = title.trim();
        redis.opsForValue().set(getCacheKey(chatId, "title"), trimmedTitle, CACHE_TTL);
        logger.info("Title stored in cache for chat: {}", chatId);

        // Transition to Step 2
        chatStateService.setState(chatId, STEP_2);
        sendMessage(chatId, "✅ Назва збережена.\nКрок 2: Опис (введи '-' для пропуску):", client);
        logger.info("Transitioned to STEP_2 for chat: {}", chatId);
    }

    /**
     * Step 2: Collect event description (optional)
     */
    private void handleStep2(long chatId, String description, TelegramClient client)
            throws TelegramApiException {
        logger.debug("Handling Step 2 (description collection) for chat: {}", chatId);

        // Retrieve title from cache
        String title = redis.opsForValue().get(getCacheKey(chatId, "title"));
        if (title == null) {
            logger.error("Step 2 failed: title not found in cache for chat: {} - session may have expired", chatId);
            cleanup(chatId);
            sendMessage(chatId, "❌ Сесія істекла. Натисни /start для запуску:", client);
            return;
        }

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
        sendMessage(chatId, "✅ Опис збережений.\nКрок 3: Дата та час (YYYY-MM-DD HH:mm) або введи '-':", client);
        logger.info("Transitioned to STEP_3 for chat: {}", chatId);
    }

    /**
     * Step 3: Collect event date/time and create event
     */
    private void handleStep3(long chatId, String datetimeStr, TelegramClient client)
            throws TelegramApiException {
        logger.debug("Handling Step 3 (datetime and event creation) for chat: {}", chatId);

        // Retrieve title and description from cache
        String title = redis.opsForValue().get(getCacheKey(chatId, "title"));
        String description = redis.opsForValue().get(getCacheKey(chatId, "description"));

        if (title == null) {
            logger.error("Step 3 failed: title not found in cache for chat: {} - session may have expired", chatId);
            cleanup(chatId);
            sendMessage(chatId, "❌ Сесія істекла. Натисни /start для запуску:", client);
            return;
        }

        // Parse datetime or set to null
        LocalDateTime eventDate = null;
        if (datetimeStr != null && !"-".equals(datetimeStr.trim())) {
            try {
                eventDate = LocalDateTime.parse(datetimeStr.trim(), DATETIME_FORMATTER);
                logger.info("Datetime parsed successfully for chat: {} - {}", chatId, eventDate);
            } catch (DateTimeParseException e) {
                logger.warn("Invalid datetime format for chat: {}: '{}' - {}", chatId, datetimeStr, e.getMessage());
                sendMessage(chatId, "❌ Невірний формат дати. Спробуй (YYYY-MM-DD HH:mm):", client);
                return;
            }
        } else {
            logger.info("Datetime skipped (dash) for chat: {}", chatId);
        }

        // Create EventDTO (userId is NULL - service will fill it from tgId)
        EventDTO eventDTO = EventDTO.builder()
            .userId(null)                          // Service will set from tgId
            .title(title)
            .description(description)              // Can be null
            .eventDate(eventDate)                  // Can be null
            .build();

        try {
            logger.info("Creating event for chat: {} with title: '{}' and description: {}",
                chatId, title, description != null ? "'" + description + "'" : "null");

            // Service receives chatId as tgId and derives user internally
            eventService.createEvent(eventDTO, chatId);

            sendMessage(chatId, "✅ Подія створена: " + title, client);
            logger.info("Event created successfully for chat: {}", chatId);

        } catch (EventNotCreatedException e) {
            logger.error("EventNotCreatedException while creating event for chat: {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "❌ Помилка при створенні подій. Спробуй ще раз (/start).", client);
        } catch (UserNotCreatedException e) {
            logger.error("UserNotCreatedException while creating event for chat: {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "❌ Помилка з користувачем. Спробуй ще раз (/start).", client);
        } catch (Exception e) {
            logger.error("Unexpected error while creating event for chat: {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "❌ Непередбачена помилка. Спробуй ще раз (/start).", client);
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
}

