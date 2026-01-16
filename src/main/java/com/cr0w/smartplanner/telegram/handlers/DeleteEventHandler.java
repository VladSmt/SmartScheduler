package com.cr0w.smartplanner.telegram.handlers;

import com.cr0w.smartplanner.dto.EventDTO;
import com.cr0w.smartplanner.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class DeleteEventHandler implements BotCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeleteEventHandler.class);

    @Autowired
    private EventService eventService;

    @Override
    public boolean canHandle(Update update) {
        return update.hasCallbackQuery()
                && update.getCallbackQuery().getData() != null
                && update.getCallbackQuery().getData().startsWith("delete_");
    }

    @Override
    public void handle(Update update, TelegramClient client) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        String callbackQueryId = update.getCallbackQuery().getId();

        logger.info("DeleteEventHandler triggered for chat: {}, data: {}", chatId, callbackData);

        try {
            // 1. Парсимо ID (формат "delete_123")
            String idString = callbackData.substring("delete_".length());
            Long eventId = Long.parseLong(idString);

            // 2. Видаляємо через сервіс
            EventDTO deletedEvent = eventService.deleteEvent(eventId);

            String responseText;
            if (deletedEvent != null) {
                responseText = String.format("🗑 <b>Подію \"%s\" успішно видалено.</b>", deletedEvent.getTitle());
                logger.info("Event id: {} deleted successfully for user in chat: {}", eventId, chatId);
            } else {
                responseText = "⚠️ Подію не знайдено (можливо, вже видалена).";
                logger.warn("Attempted to delete event id: {}, but service returned null", eventId);
            }

            // 3. Редагуємо старе повідомлення (прибираємо кнопки і міняємо текст)
            EditMessageText editMessage = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(responseText)
                    .parseMode("HTML")
                    .build();

            client.execute(editMessage);

            // 4. Обов'язково відповідаємо на колбек, щоб прибрати "годинничок" на кнопці
            AnswerCallbackQuery closeQuery = AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .text("Видалено!") // Спливаюче повідомлення (тост)
                    .build();

            client.execute(closeQuery);

        } catch (NumberFormatException e) {
            logger.error("Failed to parse event ID from data: {}", callbackData, e);
            sendAlert(client, callbackQueryId, "Помилка: некоректний ID події.");
        } catch (TelegramApiException e) {
            logger.error("Telegram API error during delete handling: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during delete handling: {}", e.getMessage(), e);
            sendAlert(client, callbackQueryId, "Сталася помилка на сервері.");
        }
    }

    // Допоміжний метод, щоб показати юзеру спливаючу помилку
    private void sendAlert(TelegramClient client, String callbackQueryId, String text) {
        try {
            client.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .text(text)
                    .showAlert(true) // true = вилізе вікно з кнопкою "ОК"
                    .build());
        } catch (TelegramApiException ex) {
            logger.error("Failed to send alert callback", ex);
        }
    }
}