package com.cr0w.smartplanner.telegram.handlers;

import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.dto.CreateEventDTO;
import com.cr0w.smartplanner.service.AIService;
import com.cr0w.smartplanner.service.EventService;
import com.cr0w.smartplanner.telegram.enums.ChatState;
import com.cr0w.smartplanner.telegram.service.ChatStateService;
import com.cr0w.smartplanner.telegram.service.TelegramService;
import com.cr0w.smartplanner.telegram.view.EventView;
import com.cr0w.smartplanner.telegram.view.KeyboardFactory;
import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@RequiredArgsConstructor
@Component
public class AIParserHandler implements CommandHandler {

    private final AIService aiService;
    private final TelegramService telegramService;
    private final EventService eventService;
    private final ChatStateService chatStateService;
    private final EventView eventView;
    private final KeyboardFactory keyboardFactory;

    @Override
    public String getCommand() {
        return ChatState.AWAITING.name();
    }

    @Override
    public void handle(Message message) {
        System.out.println("AIParserHandler handling message: " + message.getText());

        CreateEventDTO res = aiService.parseEventFromText(message.getText());
        chatStateService.setTemporaryData(message.getChatId(), res);
        chatStateService.clearState(message.getChatId());

        telegramService.sendMessage(message.getChatId(), eventView.formatConfirmation(res), keyboardFactory.createConfirmationKeyboard(message.getChatId()));
    }
}
