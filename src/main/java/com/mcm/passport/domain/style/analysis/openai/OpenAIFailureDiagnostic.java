package com.mcm.passport.domain.style.analysis.openai;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Safe failure metadata. This value deliberately cannot retain a Throwable, raw response, prompt,
 * API key, or free-form exception message.
 */
public record OpenAIFailureDiagnostic(
		String failureType,
		OpenAIFailureStage failureStage,
		OpenAIFailureDetail safeFailureDetail,
		Integer httpStatus,
		String errorCode,
		String requestId
) {

	private static final String UNKNOWN_FAILURE = "UnknownFailure";
	private static final Set<String> SAFE_FAILURE_TYPES = Set.of(
			"BadRequestException",
			"HttpTimeoutException",
			"IllegalArgumentException",
			"IllegalStateException",
			"InternalServerException",
			"InterruptedIOException",
			"NotFoundException",
			"NullPointerException",
			"OpenAIException",
			"OpenAIHttpStatusFailure",
			"OpenAIInvalidDataException",
			"OpenAIIoException",
			"OpenAIResponseIncomplete",
			"OpenAIRetryableException",
			"OpenAIServiceException",
			"OpenAIStructuredOutputException",
			"PermissionDeniedException",
			"RateLimitException",
			"RuntimeException",
			"SocketTimeoutException",
			"SseException",
			"SubjectTokenProviderException",
			"TimeoutException",
			"UnauthorizedException",
			"UnexpectedStatusCodeException",
			UNKNOWN_FAILURE,
			"UnprocessableEntityException"
	);
	private static final Set<String> TIMEOUT_FAILURE_TYPES = Set.of(
			"HttpTimeoutException",
			"InterruptedIOException",
			"SocketTimeoutException",
			"TimeoutException"
	);
	private static final Set<String> SAFE_ERROR_CODES = Set.of(
			"content_policy_violation",
			"context_length_exceeded",
			"insufficient_quota",
			"invalid_api_key",
			"invalid_request_error",
			"invalid_value",
			"model_not_found",
			"rate_limit_exceeded",
			"request_too_large",
			"server_error",
			"string_above_max_length",
			"unsupported_value"
	);
	private static final Pattern SAFE_REQUEST_ID = Pattern.compile("req_[A-Za-z0-9_-]{1,124}");

	public OpenAIFailureDiagnostic {
		failureType = safeFailureType(failureType);
		failureStage = Objects.requireNonNull(failureStage, "failureStage must not be null");
		safeFailureDetail = Objects.requireNonNull(
				safeFailureDetail,
				"safeFailureDetail must not be null"
		);
		if (httpStatus != null && (httpStatus < 100 || httpStatus > 599)) {
			throw new IllegalArgumentException("httpStatus must be a valid HTTP status code");
		}
		errorCode = safeErrorCode(errorCode);
		requestId = safeRemoteValue(requestId, SAFE_REQUEST_ID);
	}

	public static OpenAIFailureDiagnostic of(
			Throwable failure,
			OpenAIFailureStage failureStage,
			OpenAIFailureDetail safeFailureDetail,
			Integer httpStatus,
			String errorCode,
			String requestId
	) {
		Objects.requireNonNull(failure, "failure must not be null");
		String simpleName = safeFailureType(failure.getClass().getSimpleName());
		String timeoutType = safeTimeoutCauseType(failure);
		return new OpenAIFailureDiagnostic(
				timeoutType == null ? simpleName : timeoutType,
				failureStage,
				safeFailureDetail,
				httpStatus,
				errorCode,
				requestId
		);
	}

	private static String safeFailureType(String value) {
		return value != null && SAFE_FAILURE_TYPES.contains(value) ? value : UNKNOWN_FAILURE;
	}

	private static String safeErrorCode(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return SAFE_ERROR_CODES.contains(trimmed) ? trimmed : null;
	}

	private static String safeTimeoutCauseType(Throwable failure) {
		Throwable current = failure;
		for (int depth = 0; current != null && depth < 8; depth++) {
			String type = current.getClass().getSimpleName();
			if (TIMEOUT_FAILURE_TYPES.contains(type)) {
				return type;
			}
			current = current.getCause();
		}
		return null;
	}

	private static String safeRemoteValue(String value, Pattern allowlist) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return allowlist.matcher(trimmed).matches() ? trimmed : null;
	}
}
