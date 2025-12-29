package com.cr0w.smartplanner.exception;

public class EventNotUpdatedException extends RuntimeException {

    public EventNotUpdatedException(String message) {
        super(message);
    }
    public EventNotUpdatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
