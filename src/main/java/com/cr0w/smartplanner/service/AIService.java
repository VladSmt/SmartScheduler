package com.cr0w.smartplanner.service;

import java.time.LocalDateTime;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.cr0w.smartplanner.dto.CreateEventDTO;
import com.cr0w.smartplanner.exception.UserFriendlyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final ChatModel chatModel;

    @Retryable(
            retryFor = {TransientAiException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public CreateEventDTO parseEventFromText(String userInput) {

        var converter = new BeanOutputConverter<>(CreateEventDTO.class);

        String promptMessage = """
                Ти — розумний помічник-планувальник. Твоє завдання: витягнути дані про подію з тексту.
                
                ПОТОЧНА ДАТА ТА ЧАС: {currentDateTime}
                ТЕКСТ КОРИСТУВАЧА: {userInput}
                
                ПРАВИЛА:
                1. Якщо рік не вказано — використовуй 2026 рік.
                2. Якщо час не вказано — став 12:00.
                3. Відповідай ТІЛЬКИ у форматі JSON.
                4. Якщо текст — це випадковий набір літер, нісенітниця, спам або не містить жодного сенсу, що стосується планування чи подій — ПОВЕРНИ JSON, де поле 'title' дорівнює точно "INVALID".
                {format}
                """;
        PromptTemplate template = new PromptTemplate(promptMessage);
        template.add("userInput", userInput);
        template.add("currentDateTime", LocalDateTime.now().toString());
        template.add("format", converter.getFormat());

        log.info(converter.getFormat());

        try {
            ChatResponse response = chatModel.call(template.create());
            String rawJson = response.getResult().getOutput().getText();

            log.info("AI RAW JSON: {}", rawJson);
            CreateEventDTO res = converter.convert(rawJson);

            if (res == null || "INVALID".equalsIgnoreCase(res.getTitle()) || res.getTitle() == null) {
                throw new UserFriendlyException("Бро, це не схоже на подію. Напиши щось нормальне, наприклад: 'Зустріч завтра о 10'.");
            }
            return res;
        } catch (UserFriendlyException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("AI Parsing failed: {}", e.getMessage());
            throw new UserFriendlyException("Бро, я не зміг розібрати цей текст. Спробуй написати ясніше.");
        }

    }

}
