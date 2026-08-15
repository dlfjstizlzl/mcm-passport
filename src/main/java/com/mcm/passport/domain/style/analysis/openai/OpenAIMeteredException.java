package com.mcm.passport.domain.style.analysis.openai;

import com.mcm.passport.domain.style.analysis.metrics.OpenAIUsageMetrics;

import java.util.Objects;

/** Safe metadata envelope for a failed request; no prompt or raw response is retained here. */
public class OpenAIMeteredException extends RuntimeException {

	private final OpenAIUsageMetrics usage;
	private final String model;
	private final long providerLatencyMs;
	private final OpenAIFailureDiagnostic diagnostic;

	public OpenAIMeteredException(
			OpenAIUsageMetrics usage,
			String model,
			long providerLatencyMs,
			OpenAIFailureDiagnostic diagnostic
	) {
		super("OpenAI style analysis request failed");
		this.usage = Objects.requireNonNull(usage, "usage must not be null");
		if (model == null || model.isBlank()) {
			throw new IllegalArgumentException("OpenAI response model must not be blank");
		}
		this.model = model.trim();
		if (providerLatencyMs < 0) {
			throw new IllegalArgumentException("OpenAI provider latency must not be negative");
		}
		this.providerLatencyMs = providerLatencyMs;
		this.diagnostic = Objects.requireNonNull(diagnostic, "diagnostic must not be null");
	}

	public OpenAIUsageMetrics usage() {
		return usage;
	}

	public String model() {
		return model;
	}

	public long providerLatencyMs() {
		return providerLatencyMs;
	}

	public OpenAIFailureDiagnostic diagnostic() {
		return diagnostic;
	}
}
