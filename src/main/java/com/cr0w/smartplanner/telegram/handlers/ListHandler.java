package com.cr0w.smartplanner.telegram.handlers;

import com.cr0w.smartplanner.dto.EventDTO;
import com.cr0w.smartplanner.service.EventService;
import com.cr0w.smartplanner.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Component
public class ListHandler implements BotCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(ListHandler.class);

    @Autowired
    private EventService eventService;

    @Autowired
    private UserService userService;

    @Override
    public boolean canHandle(Update update) {
        String text = update.getMessage().getText();
        return update.hasMessage() && text != null && "/list".equalsIgnoreCase(text.trim());
    }

    @Override
    public void handle(Update update, TelegramClient client) {
        long chatId = update.getMessage().getChatId();
        logger.info("ListHandler handling list request from chat: {}", chatId);

        try {
            Long userId = userService.getUserOrCreateNew(chatId).getId();
            List<EventDTO> events = eventService.getEventsByTgId(userId);

            if (events.isEmpty()) {
                sendNoEventsMessage(client, chatId);
                return;
            }

            // Основна зміна тут: проходимо по списку і шлемо окреме повідомлення для кожної події
            for (EventDTO event : events) {
                sendEventMessage(client, chatId, event);
                // Маленька пауза, щоб не зловити 429 Too Many Requests, якщо подій багато
                // Thread.sleep(50); // Можна розкоментувати, якщо подій > 20
            }

            logger.info("Successfully sent {} event messages to chat: {}", events.size(), chatId);

        } catch (TelegramApiException e) {
            logger.error("Telegram API error in chat {}: {}", chatId, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error in chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    private void sendEventMessage(TelegramClient client, long chatId, EventDTO event) throws TelegramApiException {
        String eventText = formatSingleEvent(event);
        InlineKeyboardMarkup markup = createButtonsForEvent(event);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(eventText)
                .parseMode("HTML")
                .replyMarkup(markup)
                .build();

        client.execute(message);
    }

    private void sendNoEventsMessage(TelegramClient client, long chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("📋 <b>Події</b>\n\nСпискок порожній. Ти вільний як вітер!")
                .parseMode("HTML")
                .build();
        client.execute(message);
    }

    /**
     * Форматує текст для ОДНІЄЇ події
     */
    private String formatSingleEvent(EventDTO event) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("<b>📌 %s</b>\n", escapeHtml(event.getTitle())));

        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            sb.append(String.format("<i>%s</i>\n", escapeHtml(event.getDescription())));
        }

        if (event.getEventDate() != null) {
            sb.append(String.format("📅 <code>%s</code>\n", event.getEventDate()));
        }

        return sb.toString();
    }

    /**
     * Створює кнопки тільки для цієї конкретної події
     */
    private InlineKeyboardMarkup createButtonsForEvent(EventDTO event) {
        InlineKeyboardButton editButton = InlineKeyboardButton.builder()
                .text("✏️ Ред.")
                .callbackData("edit_" + event.getId())
                .build();

        InlineKeyboardButton deleteButton = InlineKeyboardButton.builder()
                .text("🗑️ Видал.")
                .callbackData("delete_" + event.getId())
                .build();

        // У новій версії TelegramBots це робиться через InlineKeyboardRow
        InlineKeyboardRow row = new InlineKeyboardRow(editButton, deleteButton);

        return InlineKeyboardMarkup.builder()
                .keyboardRow(row)
                .build();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}