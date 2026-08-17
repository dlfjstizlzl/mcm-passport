package com.mcm.passport.domain.demo.benchmark;

import com.mcm.passport.domain.style.analysis.openai.OpenAIStructuredOutputException;
import com.mcm.passport.domain.style.analysis.openai.OpenAIFailureDiagnostic;
import com.mcm.passport.domain.style.analysis.openai.OpenAIFailureStage;
import com.openai.errors.NotFoundException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.errors.PermissionDeniedException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

public final class OpenAIBenchmarkErrorClassifier {

	public OpenAIBenchmarkErrorCategory classify(Throwable failure) {
		return classify(failure, null);
	}

	public OpenAIBenchmarkErrorCategory classify(
			Throwable failure,
			OpenAIFailureDiagnostic diagnostic
	) {
		if (diagnostic != null) {
			OpenAIBenchmarkErrorCategory stageCategory = classifyStage(diagnostic.failureStage());
			if (stageCategory != null) {
				return stageCategory;
			}
			OpenAIBenchmarkErrorCategory httpCategory = classifySafeHttpDiagnostic(diagnostic);
			if (httpCategory != null) {
				return httpCategory;
			}
		}
		if (contains(failure, UnauthorizedException.class)) {
			return OpenAIBenchmarkErrorCategory.AUTHENTICATION;
		}
		if (contains(failure, RateLimitException.class)) {
			return OpenAIBenchmarkErrorCategory.RATE_LIMIT;
		}
		if (contains(failure, PermissionDeniedException.class)
				|| contains(failure, NotFoundException.class)) {
			return OpenAIBenchmarkErrorCategory.MODEL_UNAVAILABLE;
		}
		if (contains(failure, OpenAIStructuredOutputException.class)) {
			return OpenAIBenchmarkErrorCategory.STRUCTURED_OUTPUT_PARSE;
		}
		if (contains(failure, SocketTimeoutException.class)
				|| contains(failure, HttpTimeoutException.class)
				|| contains(failure, TimeoutException.class)) {
			return OpenAIBenchmarkErrorCategory.TIMEOUT;
		}
		if (contains(failure, OpenAIIoException.class)) {
			return contains(failure, InterruptedIOException.class)
					? OpenAIBenchmarkErrorCategory.TIMEOUT
					: OpenAIBenchmarkErrorCategory.NETWORK;
		}
		if (contains(failure, OpenAIInvalidDataException.class)) {
			return OpenAIBenchmarkErrorCategory.UNKNOWN;
		}
		if (contains(failure, IllegalArgumentException.class)) {
			return OpenAIBenchmarkErrorCategory.VALIDATION;
		}
		return OpenAIBenchmarkErrorCategory.UNKNOWN;
	}

	private OpenAIBenchmarkErrorCategory classifyStage(OpenAIFailureStage stage) {
		return switch (stage) {
			case SDK_RESPONSE_DESERIALIZATION -> OpenAIBenchmarkErrorCategory.SDK_RESPONSE_DESERIALIZATION;
			case USAGE_MAPPING -> OpenAIBenchmarkErrorCategory.USAGE_MAPPING;
			case RESPONSE_METADATA -> OpenAIBenchmarkErrorCategory.RESPONSE_METADATA;
			case STRUCTURED_OUTPUT_DESERIALIZATION -> OpenAIBenchmarkErrorCategory.STRUCTURED_OUTPUT_PARSE;
			case VALIDATION -> OpenAIBenchmarkErrorCategory.VALIDATION;
			case HTTP_REQUEST -> null;
		};
	}

	private OpenAIBenchmarkErrorCategory classifySafeHttpDiagnostic(
			OpenAIFailureDiagnostic diagnostic
	) {
		Integer status = diagnostic.httpStatus();
		if (status != null) {
			if (status == 401) {
				return OpenAIBenchmarkErrorCategory.AUTHENTICATION;
			}
			if (status == 429) {
				return OpenAIBenchmarkErrorCategory.RATE_LIMIT;
			}
			if (status == 403 || status == 404) {
				return OpenAIBenchmarkErrorCategory.MODEL_UNAVAILABLE;
			}
		}

		OpenAIBenchmarkErrorCategory errorCodeCategory = switch (diagnostic.errorCode()) {
			case "invalid_api_key" -> OpenAIBenchmarkErrorCategory.AUTHENTICATION;
			case "insufficient_quota", "rate_limit_exceeded" ->
					OpenAIBenchmarkErrorCategory.RATE_LIMIT;
			case "model_not_found" -> OpenAIBenchmarkErrorCategory.MODEL_UNAVAILABLE;
			case null, default -> null;
		};
		if (errorCodeCategory != null) {
			return errorCodeCategory;
		}

		return switch (diagnostic.failureType()) {
			case "UnauthorizedException" -> OpenAIBenchmarkErrorCategory.AUTHENTICATION;
			case "RateLimitException" -> OpenAIBenchmarkErrorCategory.RATE_LIMIT;
			case "PermissionDeniedException", "NotFoundException" ->
					OpenAIBenchmarkErrorCategory.MODEL_UNAVAILABLE;
			case "SocketTimeoutException", "HttpTimeoutException", "TimeoutException",
					"InterruptedIOException" -> OpenAIBenchmarkErrorCategory.TIMEOUT;
			case "OpenAIIoException" -> OpenAIBenchmarkErrorCategory.NETWORK;
			default -> null;
		};
	}

	private boolean contains(Throwable failure, Class<? extends Throwable> type) {
		Throwable current = failure;
		for (int depth = 0; current != null && depth < 16; depth++) {
			if (type.isInstance(current)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
