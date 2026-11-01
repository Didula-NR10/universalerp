package com.erp.common;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail authentication(AuthenticationException ex) { return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password"); }
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalid(IllegalArgumentException ex) { return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage()); }

    @ExceptionHandler(EntityNotFoundException.class)
    ProblemDetail missing(EntityNotFoundException ex) { return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage()); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(DataIntegrityViolationException ex) { return problem(HttpStatus.CONFLICT, "DATA_CONFLICT", "The operation conflicts with existing data"); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex) { return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed"); }

    private ProblemDetail problem(HttpStatus status, String code, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setProperty("code", code);
        return detail;
    }
}
