package com.cr0w.smartplanner.telegram.handlers;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.service.UserService;
import com.cr0w.smartplanner.telegram.service.MessageService;
import com.cr0w.smartplanner.telegram.service.TelegramService;
import com.cr0w.smartplanner.telegram.view.KeyboardFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Slf4j
@RequiredArgsConstructor
@Component
public class StartHandler implements CommandHandler {

    private final UserService userService;
    private final TelegramService telegramService;
    private final MessageService messageService;
    private final KeyboardFactory keyboardFactory;
    private final ChatModel chatModel;

    private final String WELCOME_MESSAGE = "welcome.uk";

    @Override
    public String getCommand() {
        return "/start";
    }

    @Override
    public void handle(Message message) {
        log.debug("Received start command from chatId: {}", message.getChatId());

        userService.getUserOrCreateNew(message.getFrom().getId());
        telegramService.sendMessage(message.getChatId(), messageService.getMessage(WELCOME_MESSAGE), keyboardFactory.createStartKeyboard());
    }
}
