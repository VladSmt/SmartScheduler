package com.cr0w.smartplanner.telegram;

import com.cr0w.smartplanner.telegram.handlers.BotCommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Component
public class CommandDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(CommandDispatcher.class);

    private final List<BotCommandHandler> handlers;

    public CommandDispatcher(List<BotCommandHandler> handlers) {
        this.handlers = handlers;
        logger.info("CommandDispatcher initialized with {} handlers", handlers.size());
    }

    public void dispatch(Update update, TelegramClient client) {
        logger.debug("Processing update: {}", update.getUpdateId());

        for (BotCommandHandler handler : handlers) {
            if (handler.canHandle(update)) {
                logger.info("Dispatching to handler: {}", handler.getClass().getSimpleName());
                handler.handle(update, client);
                return;
            }
        }
        // Якщо ніхто не обробив
        String message = update.hasMessage() ? update.getMessage().getText() : "unknown";
        logger.warn("No handler found for update: {}. Message: {}", update.getUpdateId(), message);
    }
}
