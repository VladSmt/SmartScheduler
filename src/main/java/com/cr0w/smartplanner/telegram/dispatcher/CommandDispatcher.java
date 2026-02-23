package com.cr0w.smartplanner.telegram.dispatcher;

import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.telegram.handlers.NaturalLanguageHandler;
import com.cr0w.smartplanner.telegram.handlers.TelegramErrorHandler;
import com.cr0w.smartplanner.telegram.router.CallbackRouter;
import com.cr0w.smartplanner.telegram.router.CommandRouter;
import com.cr0w.smartplanner.telegram.router.StateRouter;
import com.cr0w.smartplanner.telegram.service.ChatStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommandDispatcher {

    private final CommandRouter commandRouter;
    private final CallbackRouter callbackRouter;
    private final StateRouter stateRouter;
    private final ChatStateService chatStateService;
    private final TelegramErrorHandler errorHandler;
    private final NaturalLanguageHandler naturalLanguageHandler;

    public void dispatch(Update update) {
        try {
            Long chatId = extractChatId(update);
            if (chatId == null) return;

            if (hasGlobalCommand(update)) {
                log.debug("Global command override for {}", chatId);
                chatStateService.clearState(chatId);
                commandRouter.route(update.getMessage());
                return;
            }

            if (chatStateService.hasState(chatId)) {
                log.debug("Routing to StateRouter for user {}", chatId);
                stateRouter.route(update.getMessage());
                return;
            }

            if (update.hasCallbackQuery()) {
                callbackRouter.route(update.getCallbackQuery());
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                log.debug("Routing to MessageRouter for user {}", chatId);
                naturalLanguageHandler.handle(update.getMessage());
            } else {
                log.info("Unsupported update type for ID: {}", update.getUpdateId());
            }

        } catch (Exception e) {
            log.error("Critical error in CommandDispatcher: ", e);
            Long chatId = extractChatId(update);
            if (chatId != null) {
                chatStateService.clearState(chatId);
                chatStateService.clearTemporaryData(chatId);
            }
            errorHandler.handle(e, update);
        }
    }

    private boolean hasGlobalCommand(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return false;
        String text = update.getMessage().getText();
        return text.startsWith("/");
    }

    private Long extractChatId(Update update) {
        if (update.hasMessage()) return update.getMessage().getChatId();
        if (update.hasCallbackQuery()) return update.getCallbackQuery().getMessage().getChatId();
        return null;
    }
}
