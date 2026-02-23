package com.cr0w.smartplanner.telegram.service;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageSource messageSource;

    public String getMessage(String code) {
        return messageSource.getMessage(code, null, Locale.of("uk"));
    }

    public String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, Locale.of("uk"));
    }
}
