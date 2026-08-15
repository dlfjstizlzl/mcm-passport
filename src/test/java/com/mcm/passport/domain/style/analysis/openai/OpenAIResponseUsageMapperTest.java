package com.mcm.passport.domain.style.analysis.openai;

import com.mcm.passport.domain.style.analysis.metrics.OpenAIUsageMetrics;
import com.openai.core.JsonField;
import com.openai.models.responses.ResponseUsage;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAIResponseUsageMapperTest {

	private final OpenAIResponseUsageMapper mapper = new OpenAIResponseUsageMapper();

	@Test
	void mapsInputCachedCacheWriteOutputReasoningAndTotalTokensOffline() {
		ResponseUsage usage = ResponseUsage.builder()
				.inputTokens(1_200L)
				.inputTokensDetails(ResponseUsage.InputTokensDetails.builder()
						.cachedTokens(300L)
						.cacheWriteTokens(100L)
						.build())
				.outputTokens(250L)
				.outputTokensDetails(ResponseUsage.OutputTokensDetails.builder()
						.reasoningTokens(75L)
						.build())
				.totalTokens(1_450L)
				.build();

		OpenAIUsageMetrics mapped = mapper.map(Optional.of(usage));

		assertThat(mapped).isEqualTo(new OpenAIUsageMetrics(
				1_200L,
				300L,
				100L,
				250L,
				75L,
				1_450L
		));
	}

	@Test
	void keepsUnsupportedNestedUsageFieldsUnavailableInsteadOfInventingZero() {
		ResponseUsage usage = ResponseUsage.builder()
				.inputTokens(90L)
				.inputTokensDetails(ResponseUsage.InputTokensDetails.builder()
						.cachedTokens(JsonField.ofNullable(null))
						.cacheWriteTokens(JsonField.ofNullable(null))
						.build())
				.outputTokens(10L)
				.outputTokensDetails(ResponseUsage.OutputTokensDetails.builder()
						.reasoningTokens(JsonField.ofNullable(null))
						.build())
				.totalTokens(100L)
				.build();

		OpenAIUsageMetrics mapped = mapper.map(Optional.of(usage));

		assertThat(mapped.inputTokens()).isEqualTo(90L);
		assertThat(mapped.cachedInputTokens()).isNull();
		assertThat(mapped.cacheWriteTokens()).isNull();
		assertThat(mapped.outputTokens()).isEqualTo(10L);
		assertThat(mapped.reasoningTokens()).isNull();
		assertThat(mapped.totalTokens()).isEqualTo(100L);
	}

	@Test
	void preservesKnownZeroUsageAsZeroRatherThanUnavailable() {
		ResponseUsage usage = ResponseUsage.builder()
				.inputTokens(0L)
				.inputTokensDetails(ResponseUsage.InputTokensDetails.builder()
						.cachedTokens(0L)
						.cacheWriteTokens(0L)
						.build())
				.outputTokens(0L)
				.outputTokensDetails(ResponseUsage.OutputTokensDetails.builder()
						.reasoningTokens(0L)
						.build())
				.totalTokens(0L)
				.build();

		OpenAIUsageMetrics mapped = mapper.map(Optional.of(usage));

		assertThat(mapped).isEqualTo(new OpenAIUsageMetrics(0L, 0L, 0L, 0L, 0L, 0L));
	}

	@Test
	void keepsAllUsageUnavailableWhenResponsesApiOmitsUsage() {
		OpenAIUsageMetrics mapped = mapper.map(Optional.empty());

		assertThat(mapped).isEqualTo(OpenAIUsageMetrics.allUnavailable());
		assertThat(mapped.inputTokens()).isNull();
		assertThat(mapped.cachedInputTokens()).isNull();
		assertThat(mapped.cacheWriteTokens()).isNull();
		assertThat(mapped.outputTokens()).isNull();
		assertThat(mapped.reasoningTokens()).isNull();
		assertThat(mapped.totalTokens()).isNull();
	}
}
