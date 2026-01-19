package com.cr0w.smartplanner.mapper;

import com.cr0w.smartplanner.dto.EventDTO;
import com.cr0w.smartplanner.model.Event;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public EventDTO eventToEventDTO(Event event){
        if (event == null) return null;

        return EventDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .build();
    }

    public Event eventDTOToEvent(EventDTO eventDTO, Long userId){
        if (eventDTO == null) return null;

        return Event.builder()
                .id(eventDTO.getId())
                .userId(userId)
                .title(eventDTO.getTitle())
                .description(eventDTO.getDescription())
                .eventDate(eventDTO.getEventDate())
                .build();
    }
}
