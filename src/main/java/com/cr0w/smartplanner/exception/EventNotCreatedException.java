package com.cr0w.smartplanner.exception;

import org.hibernate.exception.DataException;
import org.springframework.dao.DataIntegrityViolationException;

public class EventNotCreatedException extends RuntimeException {
    public EventNotCreatedException(String message) {
        super(message);
    }
    public EventNotCreatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
