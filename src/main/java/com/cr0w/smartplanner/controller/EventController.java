package com.cr0w.smartplanner.controller;

import com.cr0w.smartplanner.dto.EventDTO;
import com.cr0w.smartplanner.service.EventService;
import com.cr0w.smartplanner.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST controller for Event-related operations.
 *
 * <p>This controller exposes a minimal CRUD API for Event resources. It delegates
 * business logic and error handling to {@link EventService}. The service methods
 * throw runtime exceptions (for example {@code EventNotFoundException} or
 * {@code EventNotCreatedException}) when operations fail; those exceptions are
 * intended to be handled by a global exception handler (for example a class
 * annotated with {@code @ControllerAdvice}) so that controller code remains
 * focused on request/response mapping instead of try/catch blocks.</p>
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /events - create new event (returns 201 Created)</li>
 *   <li>GET /events - get all events (returns 200 OK)</li>
 *   <li>GET /events/{id} - get event by id (returns 200 OK)</li>
 *   <li>PUT /events/{id} - update event by id (returns 200 OK)</li>
 *   <li>DELETE /events/{id} - delete event by id (returns 200 OK)</li>
 * </ul>
 * </p>
 */
@RestController
public class EventController {

    /**
     * Service that encapsulates Event business logic. Methods may throw runtime
     * exceptions which should be translated to appropriate HTTP responses by a
     * global exception handler (@ControllerAdvice).
     */
    @Autowired
    EventService eventService;

    /**
     * Create a new Event.
     *
     * @param eventDTO validated event payload
     * @return ResponseEntity containing created {@link EventDTO} and HTTP 201 status
     * @throws com.cr0w.smartplanner.exception.EventNotCreatedException when creation fails
     */
    @PostMapping("/events/{tgId}")
    public ResponseEntity<EventDTO> createEvent(@Valid @RequestBody EventDTO eventDTO, @PathVariable Long tgId) {
        EventDTO created = eventService.createEvent(eventDTO, tgId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Retrieve an Event by its id.
     *
     * @param id the id of the event to retrieve
     * @return ResponseEntity containing the found {@link EventDTO}
     * @throws com.cr0w.smartplanner.exception.EventNotFoundException when the event is not found
     */
    @GetMapping("/events/{id}")
    public ResponseEntity<EventDTO> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    /**
     * Retrieve all Events.
     *
     * @return ResponseEntity containing a list of all {@link EventDTO} objects
     * @throws RuntimeException when retrieval fails
     */
    @GetMapping("/events")
    public ResponseEntity<List<EventDTO>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    /**
     * Update an existing Event.
     *
     * @param id the id of the event to update
     * @param eventDTO validated payload with updated fields
     * @return ResponseEntity containing the updated {@link EventDTO}
     * @throws com.cr0w.smartplanner.exception.EventNotFoundException when the event is not found
     */
    @PutMapping("/events/{id}")
    public ResponseEntity<EventDTO> updateEvent(@PathVariable Long id, @Valid @RequestBody EventDTO eventDTO) {
        return ResponseEntity.ok(eventService.updateEvent(id, eventDTO));
    }

    /**
     * Delete an Event by its id.
     *
     * @param id the id of the event to delete
     * @return ResponseEntity containing the deleted {@link EventDTO}
     * @throws com.cr0w.smartplanner.exception.EventNotFoundException when the event is not found
     */
    @DeleteMapping("/events/{id}")
    public ResponseEntity<EventDTO> deleteEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.deleteEvent(id));
    }
}
