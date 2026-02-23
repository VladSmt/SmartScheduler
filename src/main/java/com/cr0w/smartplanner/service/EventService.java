package com.cr0w.smartplanner.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.cr0w.smartplanner.dto.CreateEventDTO;
import com.cr0w.smartplanner.dto.EventDTO;
import com.cr0w.smartplanner.exception.EventNotCreatedException;
import com.cr0w.smartplanner.exception.EventNotDeletedException;
import com.cr0w.smartplanner.exception.EventNotFoundException;
import com.cr0w.smartplanner.exception.EventNotUpdatedException;
import com.cr0w.smartplanner.mapper.EventMapper;
import com.cr0w.smartplanner.model.Event;
import com.cr0w.smartplanner.model.User;
import com.cr0w.smartplanner.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RequiredArgsConstructor
@Service
public class EventService {


    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "eventDate");
    private final EventRepository repository;
    private final EventMapper mapper;
    private final UserService userService;

    /**
     * Creates a new event based on the provided EventDTO.
     *
     * @param eventDTO the event data transfer object containing event details
     * @return the created event as EventDTO
     * @throws EventNotCreatedException if the event could not be created
     */
    @Transactional
    public EventDTO createEvent(@Valid CreateEventDTO eventDTO, Long tgId) {
        log.debug("Creating event: {}", eventDTO);
        User user = userService.getUserOrCreateNew(tgId);
        Event event = mapper.toEntity(eventDTO, user.getId());

        log.debug("Mapped Event entity: {}", event.getTitle());

        return mapper.toDto(repository.save(event));

    }

    /**
     * Deletes an event by its ID.
     *
     * @param id the ID of the event to delete
     * @return the deleted event as EventDTO
     * @throws EventNotFoundException   if the event with the given ID is not found
     * @throws EventNotDeletedException if the event could not be deleted
     */
    @Transactional
    public EventDTO deleteEvent(@Min(1) Long id, Long tgId) {
        User user = userService.getUserByTgId(tgId);
        Event event = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EventNotFoundException("Event with id " + id + " not found"));

        repository.delete(event);
        return mapper.toDto(event);

    }

    /**
     * Updates an existing event by its ID with new details from EventDTO.
     * Now validates that the event belongs to the user identified by tgId (same pattern as deleteEvent).
     *
     * @param id       the ID of the event to update
     * @param eventDTO the event data transfer object containing updated details
     * @param tgId     telegram id of the user performing update
     * @return the updated event as EventDTO
     * @throws EventNotFoundException   if the event with the given ID is not found or does not belong to the user
     * @throws EventNotUpdatedException if the event could not be updated
     */
    @Transactional
    public EventDTO updateEvent(@Min(1) Long id, @Valid EventDTO eventDTO, Long tgId) {
        User user = userService.getUserByTgId(tgId);
        Event event = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EventNotFoundException("Event not found"));

        mapper.updateEventFromDto(eventDTO, event);
        return mapper.toDto(event);


    }

    /**
     * Retrieves an event by its ID.
     *
     * @param id the ID of the event to retrieve
     * @return the event as EventDTO
     * @throws EventNotFoundException if the event with the given ID is not found
     */
    @Transactional(readOnly = true)
    public EventDTO getEventById(@Min(1) Long id, Long tgId) {
        User user = userService.getUserByTgId(tgId);

        return repository.findByIdAndUserId(id, user.getId())
                .map(mapper::toDto)
                .orElseThrow(() -> {
                    log.warn("Event not found with ID: {}", id);
                    return new EventNotFoundException("Event with id " + id + " not found");
                });

    }


    /**
     * Retrieves a paginated list of events associated with a user identified by their Telegram ID.
     *
     * @param tgId       the Telegram ID of the user whose events are to be retrieved
     * @param pageNumber page number
     * @return a paginated list of EventDTO objects representing the user's events
     */
    @Transactional(readOnly = true)
    public Page<EventDTO> getEventsByTgId(Long tgId, int pageNumber) {
        log.debug("Fetching page {} for chat {}", pageNumber, tgId);
        User user = userService.getUserByTgId(tgId);

        Pageable pageable = PageRequest.of(pageNumber, DEFAULT_PAGE_SIZE, DEFAULT_SORT);
        Page<Event> events = repository.findByUserId(user.getId(), pageable);

        return events.map(mapper::toDto);
    }


}
