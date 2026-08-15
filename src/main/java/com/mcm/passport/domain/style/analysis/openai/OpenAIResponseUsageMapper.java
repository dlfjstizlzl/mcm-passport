package com.mcm.passport.domain.style.analysis.openai;

import com.mcm.passport.domain.style.analysis.metrics.OpenAIUsageMetrics;
import com.openai.models.responses.ResponseUsage;

import java.util.Objects;
import java.util.Optional;

public class OpenAIResponseUsageMapper {

	public OpenAIUsageMetrics map(Optional<ResponseUsage> optionalUsage) {
		Objects.requireNonNull(optionalUsage, "OpenAI response usage Optional must not be null");
		return optionalUsage.map(this::mapKnownFields)
				.orElseGet(OpenAIUsageMetrics::allUnavailable);
	}

	private OpenAIUsageMetrics mapKnownFields(ResponseUsage usage) {
		ResponseUsage.InputTokensDetails inputDetails = usage._inputTokensDetails()
				.asKnown()
				.orElse(null);
		ResponseUsage.OutputTokensDetails outputDetails = usage._outputTokensDetails()
				.asKnown()
				.orElse(null);

		return new OpenAIUsageMetrics(
				usage._inputTokens().asKnown().orElse(null),
				inputDetails == null ? null : inputDetails._cachedTokens().asKnown().orElse(null),
				inputDetails == null ? null : inputDetails._cacheWriteTokens().asKnown().orElse(null),
				usage._outputTokens().asKnown().orElse(null),
				outputDetails == null ? null : outputDetails._reasoningTokens().asKnown().orElse(null),
				usage._totalTokens().asKnown().orElse(null)
		);
	}
}
