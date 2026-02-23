package com.cr0w.smartplanner.telegram.handlers;

import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.service.EventService;
import com.cr0w.smartplanner.telegram.enums.ChatState;
import com.cr0w.smartplanner.telegram.service.ChatStateService;
import com.cr0w.smartplanner.telegram.service.TelegramService;
import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class AddHandler implements CommandHandler {
    private final EventService eventService;
    private final TelegramService telegramService;
    private final ChatStateService chatStateService;

    @Override
    public String getCommand() {
        return "/add";
    }

    @Override
    public void handle(Message message) {
        long chatId = message.getChatId();
        telegramService.sendMessage(chatId, "Please enter the event details in natural language. For example:\n" +
                "'Meeting with John tomorrow at 3 PM' or 'Doctor appointment on July 5th at 10 AM'.");

        chatStateService.setState(chatId, ChatState.AWAITING);
    }
}
