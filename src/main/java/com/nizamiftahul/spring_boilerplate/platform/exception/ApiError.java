package com.nizamiftahul.spring_boilerplate.platform.exception;

import java.time.Instant;

public record ApiError(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path) {
}
