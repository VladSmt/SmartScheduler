package com.cr0w.smartplanner.service;

import com.cr0w.smartplanner.dto.EventDTO;
import com.cr0w.smartplanner.exception.EventNotCreatedException;
import com.cr0w.smartplanner.exception.EventNotDeletedException;
import com.cr0w.smartplanner.exception.EventNotFoundException;
import com.cr0w.smartplanner.exception.EventNotUpdatedException;
import com.cr0w.smartplanner.mapper.EventMapper;
import com.cr0w.smartplanner.model.Event;
import com.cr0w.smartplanner.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventService {
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    /**
     * Creates a new event based on the provided EventDTO.
     * @param eventDTO the event data transfer object containing event details
     * @return the created event as EventDTO
     * @throws EventNotCreatedException if the event could not be created
     */
    public EventDTO createEvent(EventDTO eventDTO){
        try {
            logger.info("Starting event creation with title: '{}'", eventDTO.getTitle());
            logger.debug("Event details - description: '{}', date: {}", eventDTO.getDescription(), eventDTO.getEventDate());

            if (eventDTO.getTitle() == null || eventDTO.getTitle().trim().isEmpty()) {
                logger.warn("Event creation validation failed: title is null or empty");
                throw new EventNotCreatedException("Event title cannot be null or empty");
            }

            Event event = Event.builder()
                    .title(eventDTO.getTitle())
                    .description(eventDTO.getDescription())
                    .eventDate(eventDTO.getEventDate())
                    .build();

            Event saved = repository.save(event);
            if (saved.getId() == null) {
                logger.error("Event creation failed: generated ID is null");
                throw new EventNotCreatedException("Failed to create event: ID was not generated");
            }

            logger.info("Event created successfully with ID: {}", saved.getId());
            return mapper.eventToEventDTO(saved);
        } catch (EventNotCreatedException e) {
            logger.error("EventNotCreatedException occurred: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error occurred while creating event: {}", e.getMessage(), e);
            throw new EventNotCreatedException("Failed to create event: " + e.getMessage());
        }
    }

    /**
     * Deletes an event by its ID.
     * @param id the ID of the event to delete
     * @return the deleted event as EventDTO
     * @throws EventNotFoundException if the event with the given ID is not found
     * @throws EventNotDeletedException if the event could not be deleted
     */
    public EventDTO deleteEvent(Long id){
        try {
            logger.info("Attempting to delete event with ID: {}", id);

            if (id == null || id <= 0) {
                logger.warn("Delete validation failed: invalid event ID: {}", id);
                throw new EventNotFoundException("Invalid event ID: " + id);
            }

            Event event = repository.findById(id)
                    .orElseThrow(() -> {
                        logger.warn("Delete failed: event not found with ID: {}", id);
                        return new EventNotFoundException("Event with id " + id + " not found");
                    });

            logger.debug("Found event to delete - ID: {}, title: '{}'", id, event.getTitle());
            repository.deleteById(id);
            logger.info("Event deleted successfully with ID: {}", id);

            return mapper.eventToEventDTO(event);
        } catch (EventNotFoundException e) {
            logger.error("EventNotFoundException occurred: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error occurred while deleting event with ID: {}: {}", id, e.getMessage(), e);
            throw new EventNotDeletedException("Failed to delete event: " + e.getMessage());
        }
    }

    /**
     * Updates an existing event by its ID with new details from EventDTO.
     * @param id the ID of the event to update
     * @param eventDTO the event data transfer object containing updated details
     * @return the updated event as EventDTO
     * @throws EventNotFoundException if the event with the given ID is not found
     * @throws EventNotUpdatedException if the event could not be updated
     */
    public EventDTO updateEvent(Long id, EventDTO eventDTO){
        try {
            logger.info("Attempting to update event with ID: {}", id);
            logger.debug("New event data - title: '{}', description: '{}', date: {}",
                    eventDTO.getTitle(), eventDTO.getDescription(), eventDTO.getEventDate());

            if (id == null || id <= 0) {
                logger.warn("Update validation failed: invalid event ID: {}", id);
                throw new EventNotFoundException("Invalid event ID: " + id);
            }

            if (eventDTO.getTitle() == null || eventDTO.getTitle().trim().isEmpty()) {
                logger.warn("Update validation failed: title is null or empty for event ID: {}", id);
                throw new EventNotUpdatedException("Event title cannot be null or empty");
            }

            Event event = repository.findById(id)
                    .orElseThrow(() -> {
                        logger.warn("Update failed: event not found with ID: {}", id);
                        return new EventNotFoundException("Event with id " + id + " not found");
                    });

            logger.debug("Found event to update - current title: '{}', description: '{}'",
                    event.getTitle(), event.getDescription());

            event.setTitle(eventDTO.getTitle());
            event.setDescription(eventDTO.getDescription());
            event.setEventDate(eventDTO.getEventDate());

            Event updated = repository.save(event);
            logger.info("Event updated successfully with ID: {} - new title: '{}'", id, updated.getTitle());
            return mapper.eventToEventDTO(updated);
        } catch (EventNotFoundException e) {
            logger.error("EventNotFoundException occurred: {}", e.getMessage());
            throw e;
        } catch (EventNotUpdatedException e) {
            logger.error("EventNotUpdatedException occurred: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error occurred while updating event with ID: {}: {}", id, e.getMessage(), e);
            throw new EventNotUpdatedException("Failed to update event: " + e.getMessage());
        }
    }

    /**
     * Retrieves an event by its ID.
     * @param id the ID of the event to retrieve
     * @return the event as EventDTO
     * @throws EventNotFoundException if the event with the given ID is not found
     */
    public EventDTO getEventById(Long id) {
        try {
            logger.info("Retrieving event with ID: {}", id);

            if (id == null || id <= 0) {
                logger.warn("Retrieval validation failed: invalid event ID: {}", id);
                throw new EventNotFoundException("Invalid event ID: " + id);
            }

            Optional<Event> event = repository.findById(id);
            if (event.isEmpty()) {
                logger.warn("Retrieval failed: event not found with ID: {}", id);
                throw new EventNotFoundException("Event with id " + id + " not found");
            }

            logger.debug("Event retrieved successfully - ID: {}, title: '{}', description: '{}'",
                    id, event.get().getTitle(), event.get().getDescription());
            return mapper.eventToEventDTO(event.get());
        } catch (EventNotFoundException e) {
            logger.error("EventNotFoundException occurred: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error occurred while retrieving event with ID: {}: {}", id, e.getMessage(), e);
            throw new EventNotFoundException("Failed to retrieve event: " + e.getMessage());
        }
    }

    /**
     * Retrieves all events.
     * @return a list of all events as EventDTOs
     * @throws RuntimeException if unable to retrieve events from the database
     */
    public List<EventDTO> getAllEvents() {
        try {
            logger.info("Starting retrieval of all events");

            List<Event> events = repository.findAll();
            if (events.isEmpty()) {
                logger.info("No events found in the database");
                return List.of();
            }

            logger.debug("Found {} events in database, converting to DTOs", events.size());

            List<EventDTO> eventDTOs = events.stream()
                    .map(mapper::eventToEventDTO)
                    .collect(Collectors.toList());

            logger.info("Successfully retrieved and converted {} events", eventDTOs.size());
            logger.debug("Event IDs: {}", eventDTOs.stream().map(EventDTO::getId).collect(Collectors.toList()));
            return eventDTOs;
        } catch (Exception e) {
            logger.error("Failed to retrieve all events from database: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve all events: " + e.getMessage(), e);
        }
    }

    /**
     * Event repository for database operations.
     */
    @Autowired
    EventRepository repository;

    /**
     * Event mapper for converting between Event and EventDTO.
     */
    @Autowired
    EventMapper mapper;
}
