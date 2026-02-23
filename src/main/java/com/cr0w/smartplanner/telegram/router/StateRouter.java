package com.cr0w.smartplanner.telegram.router;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.telegram.enums.ChatState;
import com.cr0w.smartplanner.telegram.handlers.CommandHandler;
import com.cr0w.smartplanner.telegram.handlers.TelegramErrorHandler;
import com.cr0w.smartplanner.telegram.service.ChatStateService;
import com.cr0w.smartplanner.telegram.service.TelegramService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Slf4j
@Component
public class StateRouter {

    private final Map<String, CommandHandler> handlers;
    private final TelegramErrorHandler errorHandler;
    private final TelegramService telegramService;
    private final ChatStateService chatStateService;

    public StateRouter(List<CommandHandler> handlerList, TelegramErrorHandler errorHandler, TelegramService telegramService, ChatStateService chatStateService) {
        this.errorHandler = errorHandler;
        // Збираємо мапу один раз: ключ — команда, значення — хендлер
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(CommandHandler::getCommand, h -> h));
        this.telegramService = telegramService;
        this.chatStateService = chatStateService;
    }

    public void route(Message message) {
        String text = message.getText();
        if (text == null || text.isBlank()) return;

        ChatState chatState = chatStateService.getState(message.getChatId());

        if (chatState == ChatState.AWAITING) {
            CommandHandler handler = handlers.get(ChatState.AWAITING.name());
            if (handler != null) {
                handler.handle(message);
            } else {
                handleUnknownCommand(message);
            }
        } else {
            handleUnknownCommand(message);
        }
    }

    private void handleUnknownCommand(Message message) {
        telegramService.sendMessage(message.getChatId(), "unknown request");
    }
}
