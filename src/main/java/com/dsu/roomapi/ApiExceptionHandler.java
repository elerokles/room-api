package com.dsu.roomapi;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;

// Week 9: one class answers every exception, for every controller.
// Week 10: the security filters reach it too, through ApiAuthenticationEntryPoint.
// Week 11: and through ApiAccessDeniedHandler for 403.
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // @Valid failed: 400 with one entry per bad field.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidFields(MethodArgumentNotValidException e,
                                                  HttpServletRequest request) {
        List<ApiError.FieldMessage> fields = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.FieldMessage(f.getField(), f.getDefaultMessage()))
                .toList();
        return respond(HttpStatus.BAD_REQUEST, "request has invalid fields", request, fields);
    }

    // A service rule about the request itself, e.g. startTime must be before endTime.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException e,
                                               HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, e.getMessage(), request, List.of());
    }

    // Lab E task 3, request 40: the body is not valid JSON (e.g. capacity "ten").
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> notReadable(HttpMessageNotReadableException e,
                                                HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "request body is missing or is not valid JSON",
                request, List.of());
    }

    // Lab E task 3, request 41: wrong type in the path (GET /api/rooms/abc).
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException e,
                                                 HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "'" + e.getName() + "' has the wrong type",
                request, List.of());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException e, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, e.getMessage(), request, List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(ConflictException e, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, e.getMessage(), request, List.of());
    }

    // The database is the last guard (foreign key, unique, check). If a service check was
    // ever skipped and the database refuses, the client still gets 409, not 500.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity(DataIntegrityViolationException e,
                                              HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "the database refused the change",
                request, List.of());
    }

    // Week 10: 401 in our ApiError shape, with three messages.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> unauthorized(AuthenticationException e,
                                                 HttpServletRequest request) {
        String message = "login required";
        if (e instanceof BadCredentialsException) {
            message = "wrong username or password";
        } else if (e instanceof InvalidBearerTokenException) {
            message = "token is invalid or expired";
        }
        return respond(HttpStatus.UNAUTHORIZED, message, request, List.of());
    }

    // Week 11, rule C: our own 403, thrown by a service ("belongs to another user").
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> forbidden(ForbiddenException e, HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, e.getMessage(), request, List.of());
    }

    // Week 11, rule D: Spring's 403, from a URL rule such as hasRole("ADMIN").
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDenied(AccessDeniedException e,
                                                 HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "admin role required", request, List.of());
    }

    // The safety net: keep the status Spring already knows, hide real bugs.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> other(Exception e, HttpServletRequest request) {
        // Spring's own exceptions (unknown URL, wrong method, missing parameter)
        if (e instanceof ErrorResponse spring) {
            HttpStatus status = HttpStatus.valueOf(spring.getStatusCode().value());
            return respond(status, spring.getBody().getDetail(), request, List.of());
        }
        // a real bug: details in the log, none in the response
        log.error("Unexpected error on {} {}", request.getMethod(),
                request.getRequestURI(), e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "unexpected server error",
                request, List.of());
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, String message,
                                             HttpServletRequest request,
                                             List<ApiError.FieldMessage> fieldErrors) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status, message, request.getRequestURI(), fieldErrors));
    }
}
