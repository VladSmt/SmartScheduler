package com.cr0w.smartplanner.telegram.handlers;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.dto.EventDTO;
import com.cr0w.smartplanner.service.EventService;
import com.cr0w.smartplanner.telegram.service.TelegramService;
import com.cr0w.smartplanner.telegram.view.EventView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Handles the /list command to display user's events with pagination.
 * This is a thin handler - all formatting logic is delegated to EventMessageService.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ListHandler implements CommandHandler {

    private final EventService eventService;
    private final EventView eventView;
    private final TelegramService telegramService;

    @Override
    public String getCommand() {
        return "/list";
    }

    @Override
    public void handle(Message message) {
        long chatId = message.getChatId();
        log.info("ListHandler: displaying events for chat {}", chatId);

        try {
            Page<EventDTO> eventsPage = eventService.getEventsByTgId(chatId, 0);

            String text = eventView.formatEventList(eventsPage);
            InlineKeyboardMarkup keyboard = eventView.createListKeyboard(eventsPage);

            telegramService.sendMessage(chatId, text, keyboard);

        } catch (Exception e) {
            log.error("Failed to display event list for chat {}", chatId, e);
        }
    }

}