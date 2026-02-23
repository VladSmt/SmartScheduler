package com.cr0w.smartplanner.telegram.handlers;

import com.cr0w.smartplanner.telegram.model.CallbackResponse;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface CallbackHandler<T> {
    String getPrefix();

    Class<T> getPayloadClass();

    CallbackResponse handle(T payload, CallbackQuery query);

}
