package com.dsu.roomapi;

// Week 11: "We know who you are, but you may not do this." Thrown by a service (rule C).
// ApiExceptionHandler answers 403.
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
