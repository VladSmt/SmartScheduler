package com.cr0w.smartplanner.telegram.handlers;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.dto.EventDTO;
import com.cr0w.smartplanner.service.EventService;
import com.cr0w.smartplanner.telegram.model.CallbackResponse;
import com.cr0w.smartplanner.telegram.model.PagePayload;
import com.cr0w.smartplanner.telegram.service.TelegramService;
import com.cr0w.smartplanner.telegram.view.EventView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;


@Slf4j
@RequiredArgsConstructor
@Component
public class ListPaginationCallbackHandler implements CallbackHandler<PagePayload> {

    private final EventService eventService;
    private final TelegramService telegramService;
    private final EventView eventView;

    @Override
    public String getPrefix() {
        return "PAG";
    }

    @Override
    public Class<PagePayload> getPayloadClass() {
        return PagePayload.class;
    }

    @Override
    public CallbackResponse handle(PagePayload payload, CallbackQuery query) {
        Long chatId = query.getMessage().getChatId();
        Page<EventDTO> page = eventService.getEventsByTgId(chatId, payload.page());
        String pageText = eventView.formatEventList(page);

        var pagination = EditMessageText.builder()
                .chatId(chatId)
                .messageId(query.getMessage().getMessageId())
                .text(pageText)
                .replyMarkup(eventView.createListKeyboard(page))
                .parseMode("HTML")
                .build();
        return CallbackResponse.withAction(pagination);
    }
}
