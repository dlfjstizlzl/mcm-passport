package com.mcm.passport.domain.demo.benchmark;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAIBenchmarkSummaryTest {

	@Test
	void sameResultRateUsesTheDominantFourFieldTupleAndExcludesMatchScore() {
		List<OpenAIBenchmarkRun> runs = List.of(
				successfulRun(1, "CASE_A_WITHOUT_PRODUCT_TAG", "BERLIN", "STARK", "MOOD_A", "BG_A", 80),
				successfulRun(2, "CASE_A_WITHOUT_PRODUCT_TAG", "BERLIN", "STARK", "MOOD_A", "BG_A", 90),
				successfulRun(3, "CASE_A_WITHOUT_PRODUCT_TAG", "SEOUL", "AREN", "MOOD_B", "BG_B", 100)
		);

		OpenAIBenchmarkSummary summary = summary(runs, OpenAIBenchmarkErrorCategory.NONE);
		OpenAIBenchmarkSummary.ModelSummary model = summary.models().getFirst();
		OpenAIBenchmarkSummary.CaseSummary caseSummary =
				model.cases().get("CASE_A_WITHOUT_PRODUCT_TAG");

		assertThat(model.aggregate().sameResultRate()).isEqualByComparingTo("0.6667");
		assertThat(caseSummary.aggregate().sameResultRate()).isEqualByComparingTo("0.6667");
		assertThat(caseSummary.dominantResult())
				.isEqualTo(new OpenAIBenchmarkSummary.ResultTuple("BERLIN", "STARK", "MOOD_A", "BG_A"));
		assertThat(caseSummary.matchScore().average()).isEqualByComparingTo("90");
		assertThat(caseSummary.matchScore().min()).isEqualTo(80);
		assertThat(caseSummary.matchScore().max()).isEqualTo(100);
	}

	@Test
	void fallbackRateAndNullableMetricSamplesDoNotTreatUnavailableValuesAsZero() {
		OpenAIBenchmarkRun success = successfulRun(
				1, "CASE_A_WITHOUT_PRODUCT_TAG", "BERLIN", "STARK", "MOOD_A", "BG_A", 90
		);
		OpenAIBenchmarkRun fallback = run(
				2,
				"CASE_A_WITHOUT_PRODUCT_TAG",
				"BERLIN",
				"STARK",
				"MOOD_A",
				"BG_A",
				90,
				true,
				false,
				OpenAIBenchmarkErrorCategory.FALLBACK,
				null,
				null
		);
		OpenAIBenchmarkRun otherFailure = run(
				3,
				"CASE_A_WITHOUT_PRODUCT_TAG",
				null,
				null,
				null,
				null,
				null,
				false,
				false,
				OpenAIBenchmarkErrorCategory.TIMEOUT,
				1_200L,
				new BigDecimal("0.002")
		);

		OpenAIBenchmarkSummary summary = summary(
				List.of(success, fallback, otherFailure),
				OpenAIBenchmarkErrorCategory.COST_ESTIMATE_UNAVAILABLE
		);
		OpenAIBenchmarkSummary.Aggregate aggregate = summary.models().getFirst().aggregate();

		assertThat(aggregate.callCount()).isEqualTo(3);
		assertThat(aggregate.successCount()).isEqualTo(1);
		assertThat(aggregate.fallbackCount()).isEqualTo(1);
		assertThat(aggregate.successRate()).isEqualByComparingTo("0.3333");
		assertThat(aggregate.fallbackRate()).isEqualByComparingTo("0.3333");
		assertThat(aggregate.metricSampleCounts().get("inputTokens")).isEqualTo(2);
		assertThat(aggregate.metricSampleCounts().get("estimatedCostUsd")).isEqualTo(2);
		assertThat(aggregate.totalEstimatedCostUsd()).isNull();
		assertThat(aggregate.cityCodeDistribution()).containsOnlyKeys("BERLIN");
		assertThat(summary.failures())
				.extracting(OpenAIBenchmarkSummary.FailureSummary::errorCategory)
				.containsExactly("FALLBACK", "TIMEOUT");
	}

	@Test
	void comparesCaseAAndCaseBDominantResultsWithoutCallingEitherOneAccurate() {
		OpenAIBenchmarkSummary summary = summary(List.of(
				successfulRun(1, "CASE_A_WITHOUT_PRODUCT_TAG", "BERLIN", "STARK", "MOOD_A", "BG_A", 90),
				successfulRun(2, "CASE_B_WITH_PRODUCT_TAG", "SEOUL", "AREN", "MOOD_B", "BG_B", 91)
		), OpenAIBenchmarkErrorCategory.NONE);

		OpenAIBenchmarkSummary.ModelSummary model = summary.models().getFirst();
		assertThat(model.cases()).containsOnlyKeys(
				"CASE_A_WITHOUT_PRODUCT_TAG",
				"CASE_B_WITH_PRODUCT_TAG"
		);
		assertThat(model.productTagDifference()).isEqualTo("DIFFERENT");
	}

	private OpenAIBenchmarkSummary summary(
			List<OpenAIBenchmarkRun> runs,
			OpenAIBenchmarkErrorCategory terminationReason
	) {
		return OpenAIBenchmarkSummary.from(
				new OpenAIBenchmarkExecution(
						"2026-08-16T00:00:00Z",
						"2026-08-16T00:01:00Z",
						terminationReason,
						runs
				),
				OpenAIBenchmarkPricingSnapshot.standardShortContext()
		);
	}

	private OpenAIBenchmarkRun successfulRun(
			int runNumber,
			String caseName,
			String city,
			String product,
			String mood,
			String background,
			Integer matchScore
	) {
		return run(
				runNumber,
				caseName,
				city,
				product,
				mood,
				background,
				matchScore,
				false,
				true,
				OpenAIBenchmarkErrorCategory.NONE,
				1_000L,
				new BigDecimal("0.001")
		);
	}

	private OpenAIBenchmarkRun run(
			int runNumber,
			String caseName,
			String city,
			String product,
			String mood,
			String background,
			Integer matchScore,
			boolean usedFallback,
			boolean success,
			OpenAIBenchmarkErrorCategory errorCategory,
			Long inputTokens,
			BigDecimal estimatedCost
	) {
		Long outputTokens = inputTokens == null ? null : 100L;
		Long totalTokens = inputTokens == null ? null : inputTokens + outputTokens;
		return new OpenAIBenchmarkRun(
				runNumber,
				caseName,
				"gpt-5.6-luna",
				"none",
				city,
				product,
				mood,
				background,
				matchScore,
				usedFallback,
				inputTokens,
				inputTokens == null ? null : 100L,
				inputTokens == null ? null : 50L,
				outputTokens,
				outputTokens == null ? null : 20L,
				totalTokens,
				inputTokens == null ? null : 25L,
				30L,
				estimatedCost,
				success,
				errorCategory
		);
	}
}
