package com.cr0w.smartplanner.telegram.handlers;

import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.service.AIService;
import com.cr0w.smartplanner.telegram.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@RequiredArgsConstructor
@Slf4j
@Component
public class NaturalLanguageHandler {

    private final TelegramService telegramService;
    private final AIService aiService;

    public void handle(Message message) {
        log.debug("NaturalLanguageHandler received message: {}", message.getText());

        telegramService.sendMessage(message.getChatId(), "Джарвіс думає...");

    }
}
