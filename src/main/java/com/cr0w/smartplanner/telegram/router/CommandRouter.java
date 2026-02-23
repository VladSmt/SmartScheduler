package com.cr0w.smartplanner.telegram.router;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.telegram.handlers.CommandHandler;
import com.cr0w.smartplanner.telegram.handlers.TelegramErrorHandler;
import com.cr0w.smartplanner.telegram.service.TelegramService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Slf4j
@Component
public class CommandRouter {

    private final Map<String, CommandHandler> handlers;
    private final TelegramErrorHandler errorHandler;
    private final TelegramService telegramService;

    public CommandRouter(List<CommandHandler> handlerList, TelegramErrorHandler errorHandler, TelegramService telegramService) {
        this.errorHandler = errorHandler;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(CommandHandler::getCommand, h -> h));
        this.telegramService = telegramService;
    }

    public void route(Message message) {
        String text = message.getText();
        if (text == null || text.isBlank()) return;

        try {
            log.info("input message: {}", text);
            // 1. Виділяємо саму команду (на випадок якщо є аргументи, як "/edit 123")
            String command = text.split(" ")[0];
            log.info("routing command: {}", command);
            // 2. Шукаємо хендлер
            CommandHandler handler = handlers.get(command);
            log.info("found handler: {}", handler);
            if (handler != null) {
                handler.handle(message);
            } else {
                handleUnknownCommand(message);
            }
        } catch (Exception e) {
//                errorHandler.handle(e, message, client);
        }
    }

    private void handleUnknownCommand(Message message) {
        telegramService.sendMessage(message.getChatId(), "unknown command");
    }
}

