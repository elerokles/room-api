package com.dsu.roomapi;

// "Valid request, but it clashes with existing data." ApiExceptionHandler answers 409.
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
