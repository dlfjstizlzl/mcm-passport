package com.mcm.passport.domain.demo.benchmark;

import com.mcm.passport.domain.style.analysis.openai.OpenAIStructuredOutputException;
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
		if (contains(failure, OpenAIStructuredOutputException.class)
				|| contains(failure, OpenAIInvalidDataException.class)) {
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
		if (contains(failure, IllegalArgumentException.class)) {
			return OpenAIBenchmarkErrorCategory.VALIDATION;
		}
		return OpenAIBenchmarkErrorCategory.UNKNOWN;
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
