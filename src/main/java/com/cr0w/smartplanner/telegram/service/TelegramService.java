package com.cr0w.smartplanner.telegram.service;

import org.springframework.stereotype.Service;

import com.cr0w.smartplanner.telegram.model.CallbackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramService {

    private final TelegramClient client;


    public void sendMessage(Long chatId, String text) {
        sendMessage(chatId, text, null);
    }

    public void sendMessage(Long chatId, String text, ReplyKeyboard keyboard) {
        log.debug("Sending message to chatId {}: {}", chatId, text);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboard)
                .build();

        try {
            client.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chatId {}: {}", chatId, e.getMessage());
        }
    }

    public void deleteMessage(Integer messageId, Long chatId) {
        log.debug("Deleting message  {}", messageId);
        DeleteMessage deleteMessage = DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build();
        try {
            client.execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("Could not delete message {} in chat {}: {}", messageId, chatId, e.getMessage());
        }
    }


    public void editMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        EditMessageText edit = EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .text(text)
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboard)
                .build();
        try {
            client.execute(edit);
        } catch (TelegramApiException e) {
            handleException(chatId, e);
        }
    }

    public void sendCallbackResponse(CallbackResponse res, String queryId) {
        executeResponse(res, queryId, client);
    }

    private void executeResponse(CallbackResponse res, String queryId, TelegramClient client) {
        if (res == null) return;
        try {
            client.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(queryId)
                    .text(res.answerText())
                    .showAlert(res.showAlert())
                    .build());
            if (res.action() != null) client.execute(res.action());
        } catch (Exception e) {
            log.error("Failed to send callback response to queryId {}: {}", queryId, e.getMessage());
        }
    }

    private void handleException(Long chatId, TelegramApiException e) {
        if (e.getMessage().contains("forbidden") || e.getMessage().contains("blocked")) {
            log.warn("Юзер {} заблокував бота. Треба б помітити його в БД як inactive.", chatId);
            // Тут можна викликати userService.deactivateUser(chatId);
        } else {
            log.error("Помилка відправки повідомлення юзеру {}: {}", chatId, e.getMessage());
            // У критичних випадках — кидаємо свій BusinessException
        }
    }
}
