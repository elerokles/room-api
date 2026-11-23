package com.dsu.roomapi;

// "No such thing." ApiExceptionHandler answers 404.
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
