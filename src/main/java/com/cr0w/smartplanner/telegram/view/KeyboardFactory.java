package com.cr0w.smartplanner.telegram.view;

import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.telegram.enums.CallbackOperation;
import com.cr0w.smartplanner.telegram.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

@Component
@RequiredArgsConstructor
public class KeyboardFactory {
    // Ключі для локалізації
    private static final String KEY_SETTINGS = "common.settings";
    private static final String KEY_HELP = "common.help";
    private static final String KEY_CONFIRM = "common.confirm";
    private static final String KEY_CANCEL = "common.cancel";
    private final MessageService ms;

    public InlineKeyboardMarkup createStartKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        createButton(KEY_SETTINGS, CallbackOperation.SETTINGS),
                        createButton(KEY_HELP, CallbackOperation.HELP)
                ))
                .build();
    }

    public InlineKeyboardMarkup createConfirmationKeyboard(Long chatId) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        createButton(KEY_CONFIRM, CallbackOperation.CONFIRM, chatId),
                        createButton(KEY_CANCEL, CallbackOperation.CANCEL, chatId)
                ))
                .build();
    }

    /**
     * Створює кнопку без пейлоаду (тільки префікс)
     */
    private InlineKeyboardButton createButton(String messageKey, CallbackOperation op) {
        return createButton(messageKey, op, "");
    }

    /**
     * Створює кнопку з префіксом та даними
     */
    private InlineKeyboardButton createButton(String messageKey, CallbackOperation op, Object payload) {
        String buttonText = ms.getMessage(messageKey);

        String callbackData = createCallback(op, payload);

        return InlineKeyboardButton.builder()
                .text(buttonText)
                .callbackData(callbackData)
                .build();
    }

    private String createCallback(CallbackOperation op, Object payload) {
        if (op == CallbackOperation.CONFIRM || op == CallbackOperation.CANCEL) {
            return "EV:" + payload + ":" + op.name();
        }

        if (payload == null || payload.toString().isEmpty()) {
            return op.getPrefix();
        }
        return op.getPrefix() + ":" + payload;
    }
}
