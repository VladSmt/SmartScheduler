package com.cr0w.smartplanner.telegram.model;

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

public record CallbackResponse(
        String answerText,
        boolean showAlert,
        BotApiMethod<?> action
) {
    public static CallbackResponse update(String text, EditMessageText edit) {
        return new CallbackResponse(text, false, edit);
    }

    public static CallbackResponse ok() {
        return new CallbackResponse(null, false, null);
    }

    public static CallbackResponse withAction(BotApiMethod<?> action) {
        return new CallbackResponse(null, false, action);
    }
}