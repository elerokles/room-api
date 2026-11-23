package com.dsu.roomapi;

import java.util.List;

import org.springframework.http.HttpStatus;

// Week 9: one body for every error. Not called ErrorResponse: Spring already has that name.
public record ApiError(int status, String error, String message, String path,
                       List<FieldMessage> fieldErrors) {

    public record FieldMessage(String field, String message) { }

    public static ApiError of(HttpStatus status, String message, String path,
                              List<FieldMessage> fieldErrors) {
        return new ApiError(status.value(), status.getReasonPhrase(),
                message, path, fieldErrors);
    }
}
