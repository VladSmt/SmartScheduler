package com.cr0w.smartplanner.exception;

public class UserNotCreatedException extends RuntimeException {
    public UserNotCreatedException(String message) { super(message); }
    public UserNotCreatedException(String message, Throwable cause) { super(message, cause); }
}
