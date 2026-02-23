package com.cr0w.smartplanner.telegram.handlers;

import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.dto.CreateEventDTO;
import com.cr0w.smartplanner.service.EventService;
import com.cr0w.smartplanner.telegram.enums.CallbackOperation;
import com.cr0w.smartplanner.telegram.model.CallbackResponse;
import com.cr0w.smartplanner.telegram.model.EventPayload;
import com.cr0w.smartplanner.telegram.service.ChatStateService;
import com.cr0w.smartplanner.telegram.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventCallbackHandler implements CallbackHandler<EventPayload> {

    private final TelegramService telegramService;
    private final EventService eventService;
    private final ChatStateService chatStateService;

    @Override
    public String getPrefix() {
        return "EV";
    }

    @Override
    public Class<EventPayload> getPayloadClass() {
        return EventPayload.class;
    }

    @Override
    public CallbackResponse handle(EventPayload payload, CallbackQuery query) {
        log.info("Handling event callback with payload: {}", payload);
        Long chatId = payload.id();
        Integer messageId = query.getMessage().getMessageId();
        CallbackOperation operation = payload.action();

        switch (operation) {
            case CONFIRM -> handleConfirm(messageId, chatId);
            case CANCEL -> handleCancel(messageId, chatId);
        }

        return CallbackResponse.ok();
    }

    private void handleConfirm(Integer messageId, Long chatId) {
        log.debug("User {} confirmed creating the event.", chatId);

        CreateEventDTO event = chatStateService.getTemporaryData(chatId);
        eventService.createEvent(event, chatId);

        chatStateService.clearTemporaryData(chatId);
        telegramService.editMessage(chatId, messageId, "Event created successfully!", null);
    }

    private void handleCancel(Integer messageId, Long chatId) {
        log.debug("User {} cancel creating the event.", chatId);
        telegramService.deleteMessage(messageId, chatId);
    }
}
