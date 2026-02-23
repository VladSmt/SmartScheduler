package com.cr0w.smartplanner.telegram.handlers;

import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.exception.EventNotCreatedException;
import com.cr0w.smartplanner.exception.EventNotDeletedException;
import com.cr0w.smartplanner.exception.EventNotFoundException;
import com.cr0w.smartplanner.exception.EventNotUpdatedException;
import com.cr0w.smartplanner.exception.UserFriendlyException;
import com.cr0w.smartplanner.exception.UserNotFoundException;
import com.cr0w.smartplanner.telegram.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramErrorHandler {

    private final TelegramService telegramService;

    public void handle(Exception e, Update update) {
        Long chatId = extractChatId(update);

        switch (e) {
            case UserFriendlyException userFriendlyException -> {
                log.warn("User error for {}: {}", chatId, e.getMessage());
                telegramService.sendMessage(chatId, "⚠️ " + e.getMessage());
            }
            case EventNotFoundException eventNotFoundException -> {
                log.warn("Event not found for {}: {}", chatId, e.getMessage());
                telegramService.sendMessage(chatId, "🔎 Подію не знайдено. Перевір номер/ID і спробуй ще раз.");
            }
            case EventNotCreatedException eventNotCreatedException -> {
                log.error("Event create failed for {}: {}", chatId, e.getMessage());
                telegramService.sendMessage(chatId, "❌ Не вдалося створити подію. Перевір дані та спробуй ще раз.");
            }
            case EventNotUpdatedException eventNotUpdatedException -> {
                log.error("Event update failed for {}: {}", chatId, e.getMessage());
                telegramService.sendMessage(chatId, "❌ Не вдалося оновити подію. Перевір дані та спробуй ще раз.");
            }
            case EventNotDeletedException eventNotDeletedException -> {
                log.error("Event delete failed for {}: {}", chatId, e.getMessage());
                telegramService.sendMessage(chatId, "❌ Не вдалося видалити подію. Спробуй ще раз.");
            }
            case UserNotFoundException userNotFoundException -> {
                log.warn("User not found for {}: {}", chatId, e.getMessage());
                telegramService.sendMessage(chatId, "👤 Користувача не знайдено. Спробуй /start і повтори дію.");
            }
            case TransientAiException transientAiException -> {
                log.warn("AI Temporary error (retry possible): {}", e.getMessage());
                telegramService.sendMessage(chatId, "⏳ Сервер AI перевантажений. Спробую ще раз за мить або натисни /add пізніше.");
            }
            case NonTransientAiException nonTransientAiException -> {
                log.error("AI Permanent error (check config/quota): {}", e.getMessage());
                telegramService.sendMessage(chatId, "🚫 Вибач, я зараз не можу обробити цей текст через технічні обмеження (квота або ліміти).");
            }
            case null, default -> {
                log.error("Critical error for chat {}: ", chatId, e);
                telegramService.sendMessage(chatId, "🆘 Сталася внутрішня помилка. Ми вже її лагодимо. Спробуй пізніше");
            }
        }
    }

    private Long extractChatId(Update update) {
        if (update.hasMessage()) return update.getMessage().getChatId();
        if (update.hasCallbackQuery()) return update.getCallbackQuery().getMessage().getChatId();
        return null;
    }
}
