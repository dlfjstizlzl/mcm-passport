package com.mcm.passport.global.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
		Instant timestamp,
		int status,
		String code,
		String message,
		String path,
		List<FieldError> errors
) {

	public ErrorResponse {
		errors = errors == null ? List.of() : List.copyOf(errors);
	}

	public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
		return of(errorCode, message, path, List.of());
	}

	public static ErrorResponse of(
			ErrorCode errorCode,
			String message,
			String path,
			List<FieldError> errors
	) {
		return new ErrorResponse(
				Instant.now(),
				errorCode.getStatus().value(),
				errorCode.name(),
				message,
				path,
				errors
		);
	}

	public record FieldError(String field, String reason) {
	}
}
