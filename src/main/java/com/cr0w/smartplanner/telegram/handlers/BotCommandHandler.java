package com.cr0w.smartplanner.telegram.handlers;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public interface BotCommandHandler {
    boolean canHandle(Update update);
    void handle(Update update, TelegramClient client);
}
