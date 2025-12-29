package com.cr0w.smartplanner.exception;

public class EventNotDeletedException  extends RuntimeException {

    public EventNotDeletedException(String message) {
        super(message);
    }

    public EventNotDeletedException(String message, Throwable cause) {
        super(message, cause);
    }
}
