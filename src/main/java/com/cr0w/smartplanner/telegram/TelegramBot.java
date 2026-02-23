package com.cr0w.smartplanner.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.telegram.dispatcher.CommandDispatcher;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;


@Component
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final CommandDispatcher dispatcher;

    private final String botToken;

    public TelegramBot(CommandDispatcher dispatcher, @Value("${telegram.bot.token}") String botToken) {
        this.dispatcher = dispatcher;
        this.botToken = botToken;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingSingleThreadUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        dispatcher.dispatch(update);
    }
}
