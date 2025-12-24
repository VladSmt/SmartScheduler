package com.cr0w.smartplanner.telegram.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class AIParserHandler implements BotCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(AIParserHandler.class);

    private static final String AI_HANDLER_COMMAND = "AI-помічник";

    @Override
    public boolean canHandle(Update update) {
        boolean canHandle = update.hasMessage() &&
                AI_HANDLER_COMMAND.equalsIgnoreCase(update.getMessage().getText().trim());
        logger.debug("AIParserHandler.canHandle() - result: {}", canHandle);
        return canHandle;
    }

    @Override
    public void handle(Update update, TelegramClient client) {
        long chatId = update.getMessage().getChatId();
        logger.info("AIParserHandler handling message from chat: {}", chatId);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Включений AI‑режим. Напиши текст події …")
                .build();
        try {
            client.execute(message);
            logger.debug("AI mode message sent successfully to chat: {}", chatId);
        } catch (TelegramApiException e) {
            logger.error("Failed to send AI mode message to chat: {}", chatId, e);
        }
    }
}
