package com.cr0w.smartplanner.telegram.handlers;

import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.service.EventService;
import com.cr0w.smartplanner.telegram.enums.CallbackOperation;
import com.cr0w.smartplanner.telegram.model.CallbackResponse;
import com.cr0w.smartplanner.telegram.model.IdPayload;
import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

@RequiredArgsConstructor
@Component
public class DeleteEventCallbackHandler implements CallbackHandler<IdPayload> {

    private final EventService eventService;

    @Override
    public String getPrefix() {
        return CallbackOperation.EVENT_DELETE.getPrefix();
    }

    @Override
    public Class<IdPayload> getPayloadClass() {
        return IdPayload.class;
    }

    @Override
    public CallbackResponse handle(IdPayload payload, CallbackQuery query) {
        eventService.deleteEvent(payload.id(), query.getMessage().getChatId());

        var deleteAction = EditMessageText.builder()
                .chatId(query.getMessage().getChatId())
                .text("Deleted")
                .messageId(query.getMessage().getMessageId())
                .parseMode("HTML")
                .build();

        return CallbackResponse.update("✅ Подію успішно видалено!", deleteAction);
    }


}
