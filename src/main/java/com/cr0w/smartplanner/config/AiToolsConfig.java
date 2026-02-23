package com.cr0w.smartplanner.config;

import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import com.cr0w.smartplanner.dto.DeleteEventRequest;
import com.cr0w.smartplanner.repository.EventRepository;
import com.cr0w.smartplanner.telegram.enums.ActionType;
import com.cr0w.smartplanner.telegram.model.PendingAction;
import com.cr0w.smartplanner.telegram.service.ChatStateService;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class AiToolsConfig {


    @Bean
    @Description("Видалити подію за її ID. Викликай цю функцію, коли користувач просить щось видалити.")
    public Function<DeleteEventRequest, String> deleteEventTool(ChatStateService stateService, EventRepository repo) {
        return request -> {
            var event = repo.findById(request.id()).orElse(null);
            if (event == null) return "Подію з ID " + request.id() + " не знайдено.";

            PendingAction pending = new PendingAction(
                    ActionType.DELETE,
                    request.id(),
                    null,
                    "delete event '" + event.getTitle() + "'"
            );

            stateService.setPendingAction(pending, request.chatId());


            return "CONFIRMATION_REQUIRED: " + pending.description();
        };
    }
}
