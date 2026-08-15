package com.mcm.passport.domain.demo.benchmark;

import java.math.BigDecimal;

public record OpenAIBenchmarkRun(
		int runNumber,
		String caseName,
		String model,
		String reasoningEffort,
		String cityCode,
		String recommendedProduct,
		String styleMood,
		String background,
		Integer matchScore,
		boolean usedFallback,
		Long inputTokens,
		Long cachedInputTokens,
		Long cacheWriteTokens,
		Long outputTokens,
		Long reasoningTokens,
		Long totalTokens,
		Long providerLatencyMs,
		long endToEndLatencyMs,
		BigDecimal estimatedCostUsd,
		boolean success,
		OpenAIBenchmarkErrorCategory errorCategory
) {
}
