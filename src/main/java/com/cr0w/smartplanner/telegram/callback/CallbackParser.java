package com.cr0w.smartplanner.telegram.callback;

import org.springframework.stereotype.Component;

import com.cr0w.smartplanner.telegram.enums.CallbackOperation;
import com.cr0w.smartplanner.telegram.model.EventPayload;
import com.cr0w.smartplanner.telegram.model.IdPayload;
import com.cr0w.smartplanner.telegram.model.PagePayload;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CallbackParser {
    private static final String SEPARATOR = ":";

    public String getPrefix(String data) {
        if (data == null) return "";
        return data.split(SEPARATOR)[0];
    }


    @SuppressWarnings("unchecked")
    public <T> T parse(String data, Class<T> targetClass) {
        String[] parts = data.split(SEPARATOR);

        if (targetClass == IdPayload.class) {
            return (T) new IdPayload(Long.parseLong(parts[1]));
        }
        if (targetClass == EventPayload.class) {
            return (T) new EventPayload(Long.parseLong(parts[1]), CallbackOperation.valueOf(parts[2]));
        }
        if (targetClass == PagePayload.class) {
            return (T) new PagePayload(Integer.parseInt(parts[1]));
        }

        throw new IllegalArgumentException("Unsupported payload: " + targetClass.getSimpleName());
    }
}

