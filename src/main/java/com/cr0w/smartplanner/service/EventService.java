package com.cr0w.smartplanner.service;

import com.cr0w.smartplanner.dto.EventDTO;
import com.cr0w.smartplanner.exception.EventNotCreatedException;
import com.cr0w.smartplanner.exception.EventNotDeletedException;
import com.cr0w.smartplanner.exception.EventNotFoundException;
import com.cr0w.smartplanner.exception.EventNotUpdatedException;
import com.cr0w.smartplanner.mapper.EventMapper;
import com.cr0w.smartplanner.model.Event;
import com.cr0w.smartplanner.model.User;
import com.cr0w.smartplanner.repository.EventRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.stream.Collectors;

@Validated
@RequiredArgsConstructor
@Service
public class EventService {
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);



    private final EventRepository repository;

    private final EventMapper mapper;

    private final UserService userService;

    /**
     * Creates a new event based on the provided EventDTO.
     * @param eventDTO the event data transfer object containing event details
     * @return the created event as EventDTO
     * @throws EventNotCreatedException if the event could not be created
     */
    @Transactional
    public EventDTO createEvent(@Valid EventDTO eventDTO, Long tgId) {

        logger.info("Starting event creation with title: '{}'", eventDTO.getTitle());
        logger.debug("Event details - description: '{}', date: {}", eventDTO.getDescription(), eventDTO.getEventDate());

        User user = userService.getUserOrCreateNew(tgId);
        logger.debug("User derived from tgId: {} -> userId: {}", tgId, user.getId());

        Event event = mapper.eventDTOToEvent(eventDTO, user.getId());

            try{
                Event saved = repository.save(event);
                logger.info("Event created successfully with ID: {} for user: {}", saved.getId(), user.getId());
                return mapper.eventToEventDTO(saved);
            } catch (DataAccessException e) {
                logger.error("createEvent DB Error: {}", e.getMessage());
                throw new EventNotCreatedException("Database is down or busy", e);
            } catch (Exception e) {
                logger.error("Unexpected error while creating event for user {}: {}", user.getId(), e.getMessage(), e);
                throw new EventNotCreatedException("Failed to create event", e);
            }



    }

    /**
     * Deletes an event by its ID.
     * @param id the ID of the event to delete
     * @return the deleted event as EventDTO
     * @throws EventNotFoundException if the event with the given ID is not found
     * @throws EventNotDeletedException if the event could not be deleted
     */
    @Transactional
    public EventDTO deleteEvent(@Min(1) Long id, Long tgId) {
        logger.info("Attempting to delete event with ID: {}", id);
        User user = userService.getUserByTgId(tgId);
        try {
            Event event = repository.findById(id)
                    .orElseThrow(() -> {
                        logger.warn("Delete failed: event not found with ID: {}", id);
                        return new EventNotFoundException("Event with id " + id + " not found");
                    });

            logger.debug("Found event to delete - ID: {}, title: '{}'", id, event.getTitle());
            repository.deleteByIdAndUserId(id, user.getId());
            logger.info("Event deleted successfully with ID: {}", id);

            return mapper.eventToEventDTO(event);
        } catch (DataAccessException e) {
            logger.error("deleteEvent DB Error: {}", e.getMessage());
            throw new EventNotDeletedException("Database is down or busy", e);
        } catch (Exception e) {
            logger.error("Unexpected error while deleting event {}: {}", id, e.getMessage(), e);
            throw new EventNotDeletedException("Failed to delete event with id " + id, e);
        }
    }

    /**
     * Updates an existing event by its ID with new details from EventDTO.
     * Now validates that the event belongs to the user identified by tgId (same pattern as deleteEvent).
     * @param id the ID of the event to update
     * @param eventDTO the event data transfer object containing updated details
     * @param tgId telegram id of the user performing update
     * @return the updated event as EventDTO
     * @throws EventNotFoundException if the event with the given ID is not found or does not belong to the user
     * @throws EventNotUpdatedException if the event could not be updated
     */
    @Transactional
    public EventDTO updateEvent(@Min(1) Long id, @Valid EventDTO eventDTO, Long tgId){
        logger.info("Attempting to update event with ID: {} by tgId: {}", id, tgId);
        logger.debug("New event data - title: '{}', description: '{}', date: {}",
                eventDTO.getTitle(), eventDTO.getDescription(), eventDTO.getEventDate());


        try {
            User user = userService.getUserByTgId(tgId);

            Event event = repository.findByIdAndUserId(id, user.getId())
                    .orElseThrow(() -> {
                        logger.warn("Update failed: event not found with ID: {}", id);
                        return new EventNotFoundException("Event with id " + id + " not found");
                    });

            Event updated = repository.save(mapper.eventDTOToEvent(eventDTO, user.getId()));
            logger.info("Event updated successfully with ID: {} - new title: '{}'", id, updated.getTitle());
            return mapper.eventToEventDTO(updated);
        } catch (DataAccessException e) {
            logger.error("updateEvent DB Error: {}", e.getMessage());
            throw new EventNotUpdatedException("Database is down or busy", e);
        } catch (Exception e) {
            logger.error("Unexpected error while updating event {}: {}", id, e.getMessage(), e);
            throw new EventNotUpdatedException("Failed to update event with id " + id, e);
        }

    }

    /**
     * Retrieves an event by its ID.
     * @param id the ID of the event to retrieve
     * @return the event as EventDTO
     * @throws EventNotFoundException if the event with the given ID is not found
     */
    @Transactional(readOnly = true)
    public EventDTO getEventById(@Min(1) Long id, Long tgId) {
        logger.info("Retrieving event with ID: {}", id);

        try {
            User user = userService.getUserByTgId(tgId);
            EventDTO eventDTO = repository.findByIdAndUserId(id, user.getId())
                    .map(mapper::eventToEventDTO)
                    .orElseThrow(() -> {
                        logger.warn("Event not found with ID: {}", id);
                        return new EventNotFoundException("Event with id " + id + " not found");
                    });

            logger.debug("Event retrieved successfully - ID: {}, title: '{}', description: '{}'",
                    id, eventDTO.getTitle(), eventDTO.getDescription());
            return eventDTO;
        } catch (DataAccessException e) {
            logger.error("getEventById DB Error: {}", e.getMessage());
            throw new EventNotFoundException("Database is down or busy", e);
        } catch (EventNotFoundException enf) {
            throw enf;
        } catch (Exception e) {
            logger.error("Unexpected error while retrieving event {}: {}", id, e.getMessage(), e);
            throw new EventNotFoundException("Failed to retrieve event with id " + id, e);
        }
    }



    /**
     * Retrieves all events for a specific user by their ID.
     * @param tgId the ID of the user
     * @return a list of events as EventDTOs for the specified user
     * @throws EventNotFoundException if the user has no events or unable to retrieve events from the database
     */
    @Transactional(readOnly = true)
    public List<EventDTO> getEventsByTgId(@Min(1) Long tgId) {
        logger.info("Starting retrieval of all events for user with ID: {}", tgId);

        try {
            User user = userService.getUserByTgId(tgId);
            List<Event> events = repository.findByUserId(user.getId());
            if (events.isEmpty()) {
                logger.info("No events found for user with tgID: {}", tgId);
                return List.of();
            }

            logger.debug("Found {} events for user with tgID: {}, converting to DTOs", events.size(), tgId);

            List<EventDTO> eventDTOs = events.stream()
                    .map(mapper::eventToEventDTO)
                    .collect(Collectors.toList());

            logger.info("Successfully retrieved and converted {} events for user with tgID: {}", eventDTOs.size(), tgId);
            logger.debug("Event IDs for user {}: {}", tgId, eventDTOs.stream().map(EventDTO::getId).collect(Collectors.toList()));
            return eventDTOs;
        } catch (DataAccessException e) {
            logger.error("getEventsByUserId DB Error: {}", e.getMessage());
            throw new EventNotFoundException("Database is down or busy", e);
        } catch (Exception e) {
            logger.error("Unexpected error while retrieving events for user {}: {}", tgId, e.getMessage(), e);
            throw new EventNotFoundException("Failed to retrieve events for user " + tgId, e);
        }
    }


}
