package com.cr0w.smartplanner.telegram.router;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.telegram.callback.CallbackParser;
import com.cr0w.smartplanner.telegram.handlers.CallbackHandler;
import com.cr0w.smartplanner.telegram.handlers.TelegramErrorHandler;
import com.cr0w.smartplanner.telegram.model.CallbackResponse;
import com.cr0w.smartplanner.telegram.service.TelegramService;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;


@Component
public class CallbackRouter {
    private final Map<String, CallbackHandler<?>> handlers;
    private final CallbackParser parser;
    private final TelegramErrorHandler errorHandler;
    private final TelegramService telegramService;

    public CallbackRouter(List<CallbackHandler<?>> handlerList, CallbackParser parser, TelegramErrorHandler errorHandler, TelegramService telegramService) {
        this.parser = parser;
        this.errorHandler = errorHandler;

        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(CallbackHandler::getPrefix, h -> h));
        this.telegramService = telegramService;
    }

    public void route(CallbackQuery query) {
        String prefix = parser.getPrefix(query.getData());
        CallbackHandler<?> handler = handlers.get(prefix);

        if (handler == null) return;

        Object payload = parser.parse(query.getData(), handler.getPayloadClass());
        CallbackResponse response = invoke(handler, payload, query);
        telegramService.sendCallbackResponse(response, query.getId());

    }


    @SuppressWarnings("unchecked")
    private <T> CallbackResponse invoke(CallbackHandler<T> handler, Object payload, CallbackQuery query) {
        return handler.handle((T) payload, query);
    }

}
