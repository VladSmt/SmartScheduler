package com.cr0w.smartplanner.telegram.handlers;

import com.cr0w.smartplanner.telegram.enums.ChatState;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface StateHandler {
    ChatState getState();

    void handle(Update update);
}
