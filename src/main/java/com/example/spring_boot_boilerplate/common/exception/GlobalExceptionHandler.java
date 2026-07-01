package com.example.spring_boot_boilerplate.common.exception;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.example.spring_boot_boilerplate.common.dto.ApiErrorResponse;

import java.time.OffsetDateTime;
import java.util.Arrays;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final Environment env;

    public GlobalExceptionHandler(Environment env) {
        this.env = env;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAll(Exception ex, WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiErrorResponse err = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));

        // include stacktrace in non-prod/dev-only condition
        boolean includeStack = Arrays.asList(env.getActiveProfiles()).contains("dev");
        if (includeStack) {
            err.setStackTrace(Arrays.toString(ex.getStackTrace()));
        }

        return new ResponseEntity<>(err, status);
    }

    // Example: handle specific exception
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiErrorResponse err = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));

        boolean includeStack = Arrays.asList(env.getActiveProfiles()).contains("dev");
        if (includeStack)
            err.setStackTrace(Arrays.toString(ex.getStackTrace()));

        return new ResponseEntity<>(err, status);
    }
}