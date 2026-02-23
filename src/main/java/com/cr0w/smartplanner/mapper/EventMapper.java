package com.cr0w.smartplanner.mapper;

import com.cr0w.smartplanner.dto.CreateEventDTO;
import com.cr0w.smartplanner.dto.EventDTO;
import com.cr0w.smartplanner.model.Event;
import com.cr0w.smartplanner.model.User;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface EventMapper {

    /**
     * Maps EventDTO to Event entity.
     * Note: user field is set in the @AfterMapping hook to ensure proper association.
     *
     * @param dto    the event DTO containing title, description, eventDate
     * @param userId the ID of the user who owns this event (not used in mapping, handled in @AfterMapping)
     * @return the mapped Event entity
     */
    @Mapping(target = "user", ignore = true)
    Event toEntity(EventDTO dto, Long userId);

    @Mapping(target = "user", ignore = true)
    Event toEntity(CreateEventDTO createEventDTO, Long userId);

    /**
     * Post-processing hook to set the user association after mapping.
     * This ensures userId is correctly associated with the event.
     *
     * @param dto    the source event DTO
     * @param event  the target Event entity
     * @param userId the ID of the user who owns this event
     */
    @AfterMapping
    default void setUser(EventDTO dto, @MappingTarget Event event, Long userId) {
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            event.setUser(user);
        }
    }

    @AfterMapping
    default void setUser(CreateEventDTO dto, @MappingTarget Event event, Long userId) {
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            event.setUser(user);
        }
    }

    /**
     * Updates an existing Event entity with data from EventDTO.
     * Does not modify the ID or the user association.
     *
     * @param dto    the event DTO with updated data
     * @param entity the target Event entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEventFromDto(EventDTO dto, @MappingTarget Event entity);

    /**
     * Maps Event entity to EventDTO.
     *
     * @param entity the Event entity
     * @return the mapped EventDTO
     */
    EventDTO toDto(Event entity);
}
