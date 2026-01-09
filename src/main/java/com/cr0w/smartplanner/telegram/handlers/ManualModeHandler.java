package com.cr0w.smartplanner.telegram.handlers;

import com.cr0w.smartplanner.telegram.ChatStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class ManualModeHandler implements BotCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(ManualModeHandler.class);

    @Autowired
    private ChatStateService chatStateService;

    @Override
    public boolean canHandle(Update update) {
        boolean canHandle = update.hasMessage() && "Ручний".equals(update.getMessage().getText());
        logger.debug("ManualModeHandler.canHandle() - result: {}", canHandle);
        return canHandle;
    }

    @Override
    public void handle(Update update, TelegramClient client) {
        long chatId = update.getMessage().getChatId();
        logger.info("ManualModeHandler handling manual mode request from chat: {}", chatId);

        // Set conversation state to Step 1
        chatStateService.setState(chatId, "MANUAL_MODE_STEP_1");
        logger.debug("Chat state set to MANUAL_MODE_STEP_1 for chat: {}", chatId);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Включений ручний режим. Крок 1: Назва події …")
                .build();
        try {
            client.execute(message);
            logger.info("Manual mode entered for chat: {} - state set to STEP_1", chatId);
            logger.debug("Manual mode message sent successfully to chat: {}", chatId);
        } catch (TelegramApiException e) {
            logger.error("Failed to send manual mode message to chat: {}: {}", chatId, e.getMessage(), e);
            chatStateService.clearState(chatId);  // Cleanup on error
            logger.debug("State cleared for chat: {} due to message send failure", chatId);
        }
    }
}
