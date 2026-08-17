package com.mcm.passport.domain.style.analysis.metrics;

public record OpenAIUsageMetrics(
		Long inputTokens,
		Long cachedInputTokens,
		Long cacheWriteTokens,
		Long outputTokens,
		Long reasoningTokens,
		Long totalTokens
) {

	public OpenAIUsageMetrics {
		validateNonNegative("inputTokens", inputTokens);
		validateNonNegative("cachedInputTokens", cachedInputTokens);
		validateNonNegative("cacheWriteTokens", cacheWriteTokens);
		validateNonNegative("outputTokens", outputTokens);
		validateNonNegative("reasoningTokens", reasoningTokens);
		validateNonNegative("totalTokens", totalTokens);
	}

	public static OpenAIUsageMetrics allUnavailable() {
		return new OpenAIUsageMetrics(null, null, null, null, null, null);
	}

	private static void validateNonNegative(String fieldName, Long value) {
		if (value != null && value < 0) {
			throw new IllegalArgumentException(fieldName + " must not be negative");
		}
	}
}
