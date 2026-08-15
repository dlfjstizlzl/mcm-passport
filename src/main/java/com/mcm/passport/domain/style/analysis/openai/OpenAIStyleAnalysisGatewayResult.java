package com.mcm.passport.domain.style.analysis.openai;

import com.mcm.passport.domain.style.analysis.metrics.OpenAIUsageMetrics;

import java.util.Objects;

public record OpenAIStyleAnalysisGatewayResult(
		OpenAIStyleAnalysisOutput output,
		OpenAIUsageMetrics usage,
		String model,
		long providerLatencyMs
) {

	public OpenAIStyleAnalysisGatewayResult {
		output = Objects.requireNonNull(output, "OpenAI style analysis output must not be null");
		usage = Objects.requireNonNull(usage, "OpenAI usage metrics must not be null");
		if (model == null || model.isBlank()) {
			throw new IllegalArgumentException("OpenAI response model must not be blank");
		}
		model = model.trim();
		if (providerLatencyMs < 0) {
			throw new IllegalArgumentException("OpenAI provider latency must not be negative");
		}
	}
}
