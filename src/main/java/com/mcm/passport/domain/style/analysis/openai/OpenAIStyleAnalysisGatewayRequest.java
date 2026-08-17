package com.mcm.passport.domain.style.analysis.openai;

import java.util.Objects;

public record OpenAIStyleAnalysisGatewayRequest(
		String model,
		OpenAIReasoningEffort effort,
		Long maxOutputTokens
) {

	public OpenAIStyleAnalysisGatewayRequest(String model, OpenAIReasoningEffort effort) {
		this(model, effort, null);
	}

	public OpenAIStyleAnalysisGatewayRequest {
		if (model == null || model.isBlank()) {
			throw new IllegalArgumentException("OpenAI model must not be blank");
		}
		model = model.trim();
		effort = Objects.requireNonNull(effort, "OpenAI reasoning effort must not be null");
		if (maxOutputTokens != null && maxOutputTokens <= 0) {
			throw new IllegalArgumentException("OpenAI maximum output tokens must be positive");
		}
	}
}
