package com.cr0w.smartplanner.telegram;

import com.cr0w.smartplanner.exception.EventNotFoundException;
import com.cr0w.smartplanner.telegram.handlers.BotCommandHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommandDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(CommandDispatcher.class);

    private final List<BotCommandHandler> handlers;

    public CommandDispatcher(List<BotCommandHandler> handlers) {
        this.handlers = handlers;
        logger.info("CommandDispatcher initialized with {} handlers", handlers.size());
    }

    public void dispatch(Update update, TelegramClient client) {
        logger.debug("Processing update: {}", update.getUpdateId());

        Long chatId = getChatId(update);
        try {
            for(BotCommandHandler handler : handlers){
                if(handler.canHandle(update)){
                    logger.info("Dispatching to handler: {}", handler.getClass().getSimpleName());
                    handler.handle(update, client);
                    return;
                }
            }

            if(chatId != null){
                sendErrorMessage(client, chatId, "Sry , I can't process your request 😔");
            }
        } catch (EventNotFoundException e){
            logger.warn("Resource not found: {}", e.getMessage());
            sendErrorMessage(client, chatId, "Error: event not found.");
        } catch (Exception e) {
            logger.error("Global error in Dispatcher for update {}: ", update.getUpdateId(), e);
            sendErrorMessage(client, chatId, "An unexpected error occurred. Please try again later.");
        }

    }

    private void sendErrorMessage(TelegramClient client, Long chatId, String message){
        if(chatId == null) return;
        try {
            client.execute(SendMessage.builder()
                            .chatId(chatId)
                            .text(message)
                            .build());
        } catch (ConstraintViolationException e) {
            String errorMessage = e.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(","));
            logger.warn("Validation failed: {}", errorMessage);

            sendErrorMessage(client, chatId, "Validation error: " + errorMessage);
        }

        catch (TelegramApiException e) {
            logger.error("Failed to send error message to chat {}: {}", chatId, e.getMessage());
        }
    }
    private Long getChatId(Update update){
        if(update.hasMessage()) return update.getMessage().getChatId();
        if(update.hasCallbackQuery()) return update.getCallbackQuery().getMessage().getChatId();
        return null;
    }
}
