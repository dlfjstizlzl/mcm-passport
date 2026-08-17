package com.mcm.passport.domain.style.analysis.metrics;

import com.mcm.passport.domain.style.analysis.StyleAnalysisCandidate;

import java.util.Objects;

public record OpenAIStyleAnalysisMeasurement(
		StyleAnalysisCandidate candidate,
		OpenAIUsageMetrics usage,
		String model,
		long providerLatencyMs
) {

	public OpenAIStyleAnalysisMeasurement {
		candidate = Objects.requireNonNull(candidate, "Style analysis candidate must not be null");
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
