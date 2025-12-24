package com.cr0w.smartplanner.telegram.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class StartCommandHandler implements BotCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(StartCommandHandler.class);

    @Override
    public boolean canHandle(org.telegram.telegrambots.meta.api.objects.Update update) {
        boolean canHandle = update.hasMessage() && "/start".equals(update.getMessage().getText());
        logger.debug("StartCommandHandler.canHandle() - result: {}", canHandle);
        return canHandle;
    }

    @Override
    public void handle(org.telegram.telegrambots.meta.api.objects.Update update, TelegramClient client) {
        long chatId = update.getMessage().getChatId();
        logger.info("StartCommandHandler handling /start command from chat: {}", chatId);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Привіт! Обери режим для створення подій:")
                .replyMarkup(getKeyboard())
                .build();
        try {
            client.execute(message);
            logger.debug("Start message with keyboard sent successfully to chat: {}", chatId);
        } catch (TelegramApiException e) {
            logger.error("Failed to send start message to chat: {}", chatId, e);
        }
    }

    private ReplyKeyboardMarkup getKeyboard() {
        KeyboardRow row = new KeyboardRow();
        row.add("Ручний");
        row.add("AI-помічник");

        List<KeyboardRow> keyboard = List.of(row);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
    }

}
