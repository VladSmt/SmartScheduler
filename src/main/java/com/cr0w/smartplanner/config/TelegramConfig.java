package com.cr0w.smartplanner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
@Slf4j
public class TelegramConfig {

    @Bean
    public TelegramClient telegramClient(@Value("${telegram.bot.token}") String botToken) {

        String maskedToken = botToken.substring(0, 5) + "..." + botToken.substring(botToken.length() - 4);
        log.info("Initializing TelegramClient with token: {}", maskedToken);

        try {
            TelegramClient client = new OkHttpTelegramClient(botToken);
            log.info("TelegramClient successfully initialized and connected to API.");
            return client;
        } catch (Exception e) {
            log.error("CRITICAL: Failed to initialize TelegramClient. Check your token and network connection!");
            throw new RuntimeException("TelegramClient init failed", e);
        }
    }
}
