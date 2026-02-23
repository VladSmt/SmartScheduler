package com.cr0w.smartplanner.telegram.handlers;

import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.dto.EventDTO;
import com.cr0w.smartplanner.service.EventService;
import com.cr0w.smartplanner.telegram.model.CallbackResponse;
import com.cr0w.smartplanner.telegram.model.IdPayload;
import com.cr0w.smartplanner.telegram.view.EventView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventViewHandler implements CallbackHandler<IdPayload> {

    private final EventService eventService;
    private final EventView eventView;

    @Override
    public String getPrefix() {
        return "GET"; // Наш короткий префікс для перегляду
    }

    @Override
    public Class<IdPayload> getPayloadClass() {
        return IdPayload.class; // Працюємо з простим ID
    }


    @Override
    public CallbackResponse handle(IdPayload payload, CallbackQuery query) {
        long chatId = query.getMessage().getChatId();

        // 1. Отримуємо дані. Якщо івента немає — eventService викине EventNotFoundException.
        // Ми не ловимо його тут! Його зловить глобальний ErrorHandler.
        EventDTO event = eventService.getEventById(payload.id(), chatId);

        // 2. Формуємо представлення через View
        String text = "📖 <b>Деталі події</b>\n\n" + eventView.formatEventDetail(event);

        // Тут ми можемо передати 0 як сторінку за замовчуванням,
        // або розширити IdPayload, якщо реально треба тримати сторінку.
        InlineKeyboardMarkup keyboard = eventView.createEventControlKeyboard(payload.id(), 0);

        // 3. Формуємо дію на оновлення повідомлення
        var editAction = EditMessageText.builder()
                .chatId(chatId)
                .messageId(query.getMessage().getMessageId())
                .text(text)
                .parseMode("HTML")
                .replyMarkup(keyboard)
                .build();

        // Повертаємо результат. Роутер сам зробить client.execute()
        return CallbackResponse.update("Завантажено", editAction);
    }
}