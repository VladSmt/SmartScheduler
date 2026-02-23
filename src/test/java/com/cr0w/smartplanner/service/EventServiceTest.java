//package com.cr0w.smartplanner.service;
//
//import java.lang.reflect.Method;
//import java.time.LocalDateTime;
//import java.util.Set;
//
//import jakarta.validation.ConstraintViolation;
//import jakarta.validation.Validation;
//import jakarta.validation.Validator;
//import jakarta.validation.ValidatorFactory;
//import jakarta.validation.executable.ExecutableValidator;
//
//import org.springframework.dao.DataAccessResourceFailureException;
//
//import com.cr0w.smartplanner.dto.EventDTO;
//import com.cr0w.smartplanner.exception.UserNotCreatedException;
//import com.cr0w.smartplanner.mapper.EventMapper;
//import com.cr0w.smartplanner.model.Event;
//import com.cr0w.smartplanner.model.User;
//import com.cr0w.smartplanner.repository.EventRepository;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//public class EventServiceTest {
//
//    @Mock
//    private EventRepository eventRepository;
//
//    @Mock
//    private UserService userService;
//
//    @Mock
//    private EventMapper eventMapper;
//
//    @InjectMocks
//    private EventService eventService;
//
//    @Test
//    void createEventWithValidData() {
//
//        Long tgId = 12345L;
//        User user = new User();
//        user.setId(10L);
//
//        EventDTO inputDto = EventDTO.builder()
//                .title("Test title")
//                .description("Test description")
//                .eventDate(LocalDateTime.now())
//                .build();
//
//        Event savedEntity = Event.builder()
//                .id(1L)
//                .getUser().getId(user.getId())
//                .title(inputDto.getTitle())
//                .description(inputDto.getDescription())
//                .eventDate(inputDto.getEventDate())
//                .build();
//
//        EventDTO expectedDto = EventDTO.builder()
//                .id(savedEntity.getId())
//                .title(savedEntity.getTitle())
//                .description(savedEntity.getDescription())
//                .eventDate(savedEntity.getEventDate())
//                .build();
//
//        when(userService.getUserOrCreateNew(tgId)).thenReturn(user);
//
//
//        when(eventMapper.toEntity(any(EventDTO.class), anyLong())).thenAnswer(invocation -> {
//            EventDTO dto = invocation.getArgument(0);
//            Long uid = invocation.getArgument(1);
//            return Event.builder()
//                    .userId(uid)
//                    .title(dto.getTitle())
//                    .description(dto.getDescription())
//                    .eventDate(dto.getEventDate())
//                    .build();
//        });
//
//
//        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
//            Event arg = invocation.getArgument(0);
//            // simulate DB generated id if not set
//            if (arg.getId() == null) arg.setId(savedEntity.getId());
//            return arg;
//        });
//        when(eventMapper.toDto(any(Event.class))).thenReturn(expectedDto);
//
//
//        EventDTO result = eventService.createEvent(inputDto, tgId);
//
//
//        assertEquals(expectedDto, result);
//
//        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
//        verify(eventRepository, times(1)).save(captor.capture());
//        Event persisted = captor.getValue();
//        assertEquals(user.getId(), persisted.getUserId());
//        assertEquals(inputDto.getTitle(), persisted.getTitle());
//        assertEquals(inputDto.getDescription(), persisted.getDescription());
//    }
//
//    @Test
//    void shouldThrowWhenUserCreationFails() {
//
//        Long tgId = 555L;
//        EventDTO inputDto = EventDTO.builder().title("T").build();
//
//        when(userService.getUserOrCreateNew(tgId)).thenThrow(new UserNotCreatedException("fail"));
//
//
//        Exception ex = assertThrows(Exception.class, () -> eventService.createEvent(inputDto, tgId));
//        boolean ok = ex instanceof UserNotCreatedException
//                || ex instanceof com.cr0w.smartplanner.exception.EventNotCreatedException
//                || (ex.getCause() != null && ex.getCause() instanceof UserNotCreatedException);
//        assertTrue(ok, "Expected UserNotCreatedException to be thrown or wrapped");
//
//
//        verify(eventRepository, times(0)).save(any(Event.class));
//    }
//
//    @Test
//    void shouldThrowWhenRepositorySaveFails() {
//
//        Long tgId = 999L;
//        User user = new User();
//        user.setId(20L);
//
//        EventDTO inputDto = EventDTO.builder().title("Title").build();
//
//        when(userService.getUserOrCreateNew(tgId)).thenReturn(user);
//
//        when(eventMapper.toEntity(any(EventDTO.class), anyLong())).thenAnswer(invocation -> {
//            EventDTO dto = invocation.getArgument(0);
//            Long uid = invocation.getArgument(1);
//            return Event.builder()
//                    .userId(uid)
//                    .title(dto.getTitle())
//                    .description(dto.getDescription())
//                    .eventDate(dto.getEventDate())
//                    .build();
//        });
//        when(eventRepository.save(any(Event.class))).thenThrow(new DataAccessResourceFailureException("DB down"));
//
//
//        Exception ex = assertThrows(Exception.class, () -> eventService.createEvent(inputDto, tgId));
//        boolean okSave = ex instanceof DataAccessResourceFailureException
//                || (ex instanceof com.cr0w.smartplanner.exception.EventNotCreatedException && ex.getCause() instanceof DataAccessResourceFailureException);
//        assertTrue(okSave, "Expected DataAccessResourceFailureException to be thrown or wrapped");
//    }
//
//    @Test
//    void validationShouldFailForBlankTitle() throws NoSuchMethodException {
//
//        EventDTO invalidDto = EventDTO.builder().title("   ").build();
//        long tgId = 11L;
//
//
//        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
//            Validator validator = factory.getValidator();
//            ExecutableValidator executableValidator = validator.forExecutables();
//
//            Method createMethod = EventService.class.getMethod("createEvent", EventDTO.class, Long.class);
//
//
//            Set<ConstraintViolation<EventService>> violations = executableValidator.validateParameters(
//                    eventService, createMethod, new Object[]{invalidDto, tgId}
//            );
//
//
//            assertFalse(violations.isEmpty(), "Expected constraint violations for blank title");
//            boolean found = violations.stream().anyMatch(v -> v.getMessage().contains("Title cannot be blank") || v.getMessage().contains("must not be blank"));
//            assertTrue(found, "Expected violation message about blank title");
//
//
//            verify(userService, times(0)).getUserOrCreateNew(any(Long.class));
//        }
//    }
//
//}
