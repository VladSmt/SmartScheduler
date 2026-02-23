package com.cr0w.smartplanner.telegram.handlers;

import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface CommandHandler {
    String getCommand();

    void handle(Message message);
}
