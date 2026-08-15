package com.mcm.passport.domain.demo.benchmark;

import com.mcm.passport.domain.demo.benchmark.OpenAIBenchmarkPricingSnapshot.ModelPrice;
import com.mcm.passport.domain.style.analysis.metrics.OpenAIUsageMetrics;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAIBenchmarkPricingAndCostTest {

	private final OpenAIBenchmarkPricingSnapshot pricing =
			OpenAIBenchmarkPricingSnapshot.standardShortContext();
	private final OpenAIEstimatedCostCalculator calculator = new OpenAIEstimatedCostCalculator();

	@Test
	void exposesTheCheckedStandardShortContextPriceSnapshotAndOptionalModelLookup() {
		assertThat(pricing.pricingCheckedAt()).isEqualTo(LocalDate.of(2026, 8, 16));
		assertPrice("gpt-5.6-sol", "5.00", "0.50", "6.25", "30.00");
		assertPrice("gpt-5.6-terra", "2.00", "0.20", "2.50", "12.00");
		assertPrice("gpt-5.6-luna", "0.20", "0.02", "0.25", "1.20");
		assertThat(pricing.findModel("unsupported-model")).isEmpty();
		assertThat(pricing.findModel(null)).isEmpty();
	}

	@Test
	void calculatesUncachedCachedCacheWriteAndOutputCategoriesWithoutDoubleCharging() {
		OpenAIUsageMetrics usage = new OpenAIUsageMetrics(
				100_000L,
				20_000L,
				10_000L,
				10_000L,
				5_000L,
				110_000L
		);

		assertThat(calculator.calculateUsd(usage, "gpt-5.6-luna", pricing))
				.hasValueSatisfying(cost -> assertThat(cost).isEqualByComparingTo("0.0289"));
	}

	@Test
	void reasoningTokensStayInsideOutputCostInsteadOfBeingChargedAgain() {
		OpenAIUsageMetrics withoutReasoning = new OpenAIUsageMetrics(
				1_000L, 0L, 0L, 200L, 0L, 1_200L
		);
		OpenAIUsageMetrics withReasoning = new OpenAIUsageMetrics(
				1_000L, 0L, 0L, 200L, 150L, 1_200L
		);

		BigDecimal first = calculator.calculateUsd(withoutReasoning, "gpt-5.6-terra", pricing)
				.orElseThrow();
		BigDecimal second = calculator.calculateUsd(withReasoning, "gpt-5.6-terra", pricing)
				.orElseThrow();

		assertThat(second).isEqualByComparingTo(first);
	}

	@Test
	void returnsUnavailableForMissingUsagePriceOrUnknownModel() {
		OpenAIUsageMetrics completeUsage = new OpenAIUsageMetrics(
				100L, 10L, 5L, 20L, 2L, 120L
		);
		ModelPrice incompletePrice = new ModelPrice(
				new BigDecimal("1.00"),
				null,
				new BigDecimal("1.00"),
				new BigDecimal("1.00")
		);

		assertThat(calculator.calculateUsd(OpenAIUsageMetrics.allUnavailable(), "gpt-5.6-luna", pricing))
				.isEmpty();
		assertThat(calculator.calculateUsd(completeUsage, "unsupported-model", pricing)).isEmpty();
		assertThat(calculator.calculateUsd(completeUsage, incompletePrice)).isEmpty();
	}

	@Test
	void leavesLongContextCostUnknownInsteadOfApplyingTheShortContextSnapshot() {
		OpenAIUsageMetrics usage = new OpenAIUsageMetrics(
				272_001L,
				0L,
				0L,
				10L,
				0L,
				272_011L
		);

		assertThat(calculator.calculateUsd(usage, "gpt-5.6-sol", pricing)).isEmpty();
	}

	@Test
	void reservesEachCallUsingTheMostExpensiveInputCategoryAndBoundedOutput() {
		assertThat(calculator.calculateReservationUsd(8_192L, 512L, "gpt-5.6-luna", pricing))
				.hasValueSatisfying(cost -> assertThat(cost).isEqualByComparingTo("0.0026624"));
		assertThat(calculator.calculateReservationUsd(8_192L, 512L, "gpt-5.6-terra", pricing))
				.hasValueSatisfying(cost -> assertThat(cost).isEqualByComparingTo("0.026624"));
		assertThat(calculator.calculateReservationUsd(8_192L, 512L, "gpt-5.6-sol", pricing))
				.hasValueSatisfying(cost -> assertThat(cost).isEqualByComparingTo("0.06656"));
		assertThat(calculator.calculateReservationUsd(8_192L, 512L, "unsupported-model", pricing))
				.isEmpty();
	}

	@Test
	void rejectsUsageCategoryAndTotalInclusionViolations() {
		ModelPrice luna = pricing.findModel("gpt-5.6-luna").orElseThrow();

		assertThatThrownBy(() -> calculator.calculateUsd(
				new OpenAIUsageMetrics(100L, 80L, 30L, 20L, 0L, 120L),
				luna
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("included in inputTokens");
		assertThatThrownBy(() -> calculator.calculateUsd(
				new OpenAIUsageMetrics(100L, 0L, 0L, 20L, 21L, 120L),
				luna
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("included in outputTokens");
		assertThatThrownBy(() -> calculator.calculateUsd(
				new OpenAIUsageMetrics(100L, 0L, 0L, 20L, 0L, 121L),
				luna
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("totalTokens");
	}

	@Test
	void rejectsNegativeUsageAndPricingRatherThanTreatingThemAsZero() {
		assertThatThrownBy(() -> new OpenAIUsageMetrics(-1L, 0L, 0L, 0L, 0L, 0L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("inputTokens");
		assertThatThrownBy(() -> new ModelPrice(
				new BigDecimal("-0.01"),
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("input price");
	}

	private void assertPrice(
			String model,
			String input,
			String cachedInput,
			String cacheWrite,
			String output
	) {
		ModelPrice price = pricing.findModel(model).orElseThrow();
		assertThat(price.standardInputPricePerMillion()).isEqualByComparingTo(input);
		assertThat(price.cachedInputPricePerMillion()).isEqualByComparingTo(cachedInput);
		assertThat(price.cacheWritePricePerMillion()).isEqualByComparingTo(cacheWrite);
		assertThat(price.outputPricePerMillion()).isEqualByComparingTo(output);
	}
}
