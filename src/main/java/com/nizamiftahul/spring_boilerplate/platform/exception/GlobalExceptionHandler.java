package com.nizamiftahul.spring_boilerplate.platform.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(DomainException.class)
	public ResponseEntity<ApiError> handleDomain(DomainException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return build(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
		return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
	}

	private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
		ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
		return ResponseEntity.status(status).body(body);
	}
}
