package com.mcm.passport.domain.demo.benchmark;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public record OpenAIBenchmarkSummary(
		String status,
		String startedAt,
		String completedAt,
		String terminationReason,
		String pricingCheckedAt,
		List<ModelSummary> models
) {

	public OpenAIBenchmarkSummary {
		models = List.copyOf(Objects.requireNonNull(models, "models must not be null"));
	}

	public static OpenAIBenchmarkSummary from(
			OpenAIBenchmarkExecution execution,
			OpenAIBenchmarkPricingSnapshot pricing
	) {
		Objects.requireNonNull(execution, "execution must not be null");
		Objects.requireNonNull(pricing, "pricing must not be null");
		Map<String, List<OpenAIBenchmarkRun>> byModel = orderedGroups(
				execution.runs(),
				OpenAIBenchmarkRun::model
		);
		List<ModelSummary> modelSummaries = byModel.entrySet().stream()
				.map(entry -> ModelSummary.from(entry.getKey(), entry.getValue()))
				.toList();

		return new OpenAIBenchmarkSummary(
				execution.terminationReason() == OpenAIBenchmarkErrorCategory.NONE ? "COMPLETED" : "STOPPED",
				execution.startedAt(),
				execution.completedAt(),
				execution.terminationReason().name(),
				pricing.pricingCheckedAt().toString(),
				modelSummaries
		);
	}

	public record ModelSummary(
			String model,
			Aggregate aggregate,
			Map<String, CaseSummary> cases,
			String productTagDifference
	) {

		static ModelSummary from(String model, List<OpenAIBenchmarkRun> runs) {
			Map<String, List<OpenAIBenchmarkRun>> byCase = orderedGroups(runs, OpenAIBenchmarkRun::caseName);
			Map<String, CaseSummary> cases = new LinkedHashMap<>();
			byCase.forEach((caseName, caseRuns) -> cases.put(caseName, CaseSummary.from(caseName, caseRuns)));

			ResultTuple withoutTag = dominantSuccessfulResult(byCase.get("CASE_A_WITHOUT_PRODUCT_TAG"));
			ResultTuple withTag = dominantSuccessfulResult(byCase.get("CASE_B_WITH_PRODUCT_TAG"));
			String difference = withoutTag == null || withTag == null
					? "INSUFFICIENT_DATA"
					: (withoutTag.equals(withTag) ? "SAME" : "DIFFERENT");

			return new ModelSummary(
					model,
					Aggregate.from(runs, true),
					Collections.unmodifiableMap(new LinkedHashMap<>(cases)),
					difference
			);
		}
	}

	public record CaseSummary(
			String caseName,
			Aggregate aggregate,
			ResultTuple dominantResult,
			MatchScoreSummary matchScore
	) {

		static CaseSummary from(String caseName, List<OpenAIBenchmarkRun> runs) {
			return new CaseSummary(
					caseName,
					Aggregate.from(runs, false),
					dominantSuccessfulResult(runs),
					MatchScoreSummary.from(runs)
			);
		}
	}

	public record Aggregate(
			int callCount,
			int successCount,
			int fallbackCount,
			BigDecimal successRate,
			BigDecimal fallbackRate,
			BigDecimal averageInputTokens,
			BigDecimal averageCachedInputTokens,
			BigDecimal averageCacheWriteTokens,
			BigDecimal averageOutputTokens,
			BigDecimal averageReasoningTokens,
			BigDecimal averageTotalTokens,
			BigDecimal averageProviderLatencyMs,
			Long p50ProviderLatencyMs,
			Long p95ProviderLatencyMs,
			BigDecimal averageEndToEndLatencyMs,
			Long p50EndToEndLatencyMs,
			Long p95EndToEndLatencyMs,
			BigDecimal averageEstimatedCostUsd,
			BigDecimal totalEstimatedCostUsd,
			BigDecimal sameResultRate,
			Map<String, Integer> cityCodeDistribution,
			Map<String, Integer> recommendedProductDistribution,
			Map<String, Integer> styleMoodDistribution,
			Map<String, Integer> metricSampleCounts
	) {

		static Aggregate from(List<OpenAIBenchmarkRun> runs, boolean groupConsistencyByCase) {
			List<OpenAIBenchmarkRun> successful = successful(runs);
			List<BigDecimal> knownCosts = knownDecimals(runs, OpenAIBenchmarkRun::estimatedCostUsd);
			Map<String, Integer> sampleCounts = new LinkedHashMap<>();
			registerSample(sampleCounts, "inputTokens", runs, OpenAIBenchmarkRun::inputTokens);
			registerSample(sampleCounts, "cachedInputTokens", runs, OpenAIBenchmarkRun::cachedInputTokens);
			registerSample(sampleCounts, "cacheWriteTokens", runs, OpenAIBenchmarkRun::cacheWriteTokens);
			registerSample(sampleCounts, "outputTokens", runs, OpenAIBenchmarkRun::outputTokens);
			registerSample(sampleCounts, "reasoningTokens", runs, OpenAIBenchmarkRun::reasoningTokens);
			registerSample(sampleCounts, "totalTokens", runs, OpenAIBenchmarkRun::totalTokens);
			registerSample(sampleCounts, "providerLatencyMs", runs, OpenAIBenchmarkRun::providerLatencyMs);
			sampleCounts.put("endToEndLatencyMs", runs.size());
			sampleCounts.put("estimatedCostUsd", knownCosts.size());

			return new Aggregate(
					runs.size(),
					successful.size(),
					(int) runs.stream().filter(OpenAIBenchmarkRun::usedFallback).count(),
					rate(successful.size(), runs.size()),
					rate(runs.stream().filter(OpenAIBenchmarkRun::usedFallback).count(), runs.size()),
					averageLong(runs, OpenAIBenchmarkRun::inputTokens),
					averageLong(runs, OpenAIBenchmarkRun::cachedInputTokens),
					averageLong(runs, OpenAIBenchmarkRun::cacheWriteTokens),
					averageLong(runs, OpenAIBenchmarkRun::outputTokens),
					averageLong(runs, OpenAIBenchmarkRun::reasoningTokens),
					averageLong(runs, OpenAIBenchmarkRun::totalTokens),
					averageLong(runs, OpenAIBenchmarkRun::providerLatencyMs),
					percentile(knownLongs(runs, OpenAIBenchmarkRun::providerLatencyMs), 0.50),
					percentile(knownLongs(runs, OpenAIBenchmarkRun::providerLatencyMs), 0.95),
					averageLong(runs, run -> run.endToEndLatencyMs()),
					percentile(runs.stream().map(OpenAIBenchmarkRun::endToEndLatencyMs).toList(), 0.50),
					percentile(runs.stream().map(OpenAIBenchmarkRun::endToEndLatencyMs).toList(), 0.95),
					average(knownCosts),
					knownCosts.size() == runs.size() ? sum(knownCosts) : null,
					consistencyRate(successful, groupConsistencyByCase),
					distribution(successful, OpenAIBenchmarkRun::cityCode),
					distribution(successful, OpenAIBenchmarkRun::recommendedProduct),
					distribution(successful, OpenAIBenchmarkRun::styleMood),
					Collections.unmodifiableMap(new LinkedHashMap<>(sampleCounts))
			);
		}
	}

	public record ResultTuple(
			String cityCode,
			String recommendedProduct,
			String styleMood,
			String background
	) {

		static ResultTuple from(OpenAIBenchmarkRun run) {
			return new ResultTuple(run.cityCode(), run.recommendedProduct(), run.styleMood(), run.background());
		}
	}

	public record MatchScoreSummary(BigDecimal average, Integer min, Integer max, int sampleCount) {

		static MatchScoreSummary from(List<OpenAIBenchmarkRun> runs) {
			List<Integer> scores = successful(runs).stream()
					.map(OpenAIBenchmarkRun::matchScore)
					.filter(Objects::nonNull)
					.toList();
			if (scores.isEmpty()) {
				return new MatchScoreSummary(null, null, null, 0);
			}
			BigDecimal total = scores.stream()
					.map(BigDecimal::valueOf)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			return new MatchScoreSummary(
					total.divide(BigDecimal.valueOf(scores.size()), 4, RoundingMode.HALF_UP),
					scores.stream().min(Integer::compareTo).orElseThrow(),
					scores.stream().max(Integer::compareTo).orElseThrow(),
					scores.size()
			);
		}
	}

	private static BigDecimal consistencyRate(
			List<OpenAIBenchmarkRun> successful,
			boolean groupByCase
	) {
		if (successful.isEmpty()) {
			return null;
		}
		Map<String, List<OpenAIBenchmarkRun>> groups = orderedGroups(
				successful,
				run -> groupByCase
						? run.caseName() + "\u0000" + run.reasoningEffort()
						: run.reasoningEffort()
		);
		long dominantTotal = groups.values().stream()
				.mapToLong(OpenAIBenchmarkSummary::dominantCount)
				.sum();
		return rate(dominantTotal, successful.size());
	}

	private static long dominantCount(List<OpenAIBenchmarkRun> runs) {
		return runs.stream()
				.collect(java.util.stream.Collectors.groupingBy(ResultTuple::from, java.util.stream.Collectors.counting()))
				.values().stream()
				.max(Long::compareTo)
				.orElse(0L);
	}

	private static ResultTuple dominantSuccessfulResult(List<OpenAIBenchmarkRun> runs) {
		if (runs == null) {
			return null;
		}
		return successful(runs).stream()
				.collect(java.util.stream.Collectors.groupingBy(ResultTuple::from, java.util.stream.Collectors.counting()))
				.entrySet().stream()
				.sorted(Map.Entry.<ResultTuple, Long>comparingByValue().reversed()
						.thenComparing(entry -> entry.getKey().toString()))
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(null);
	}

	private static List<OpenAIBenchmarkRun> successful(List<OpenAIBenchmarkRun> runs) {
		return runs.stream()
				.filter(OpenAIBenchmarkRun::success)
				.filter(run -> !run.usedFallback())
				.toList();
	}

	private static Map<String, Integer> distribution(
			List<OpenAIBenchmarkRun> runs,
			Function<OpenAIBenchmarkRun, String> extractor
	) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		runs.stream()
				.map(extractor)
				.filter(Objects::nonNull)
				.sorted()
				.forEach(value -> counts.merge(value, 1, Integer::sum));
		return Collections.unmodifiableMap(new LinkedHashMap<>(counts));
	}

	private static BigDecimal averageLong(
			List<OpenAIBenchmarkRun> runs,
			Function<OpenAIBenchmarkRun, Long> extractor
	) {
		return average(knownLongs(runs, extractor).stream().map(BigDecimal::valueOf).toList());
	}

	private static List<Long> knownLongs(
			List<OpenAIBenchmarkRun> runs,
			Function<OpenAIBenchmarkRun, Long> extractor
	) {
		return runs.stream().map(extractor).filter(Objects::nonNull).toList();
	}

	private static List<BigDecimal> knownDecimals(
			List<OpenAIBenchmarkRun> runs,
			Function<OpenAIBenchmarkRun, BigDecimal> extractor
	) {
		return runs.stream().map(extractor).filter(Objects::nonNull).toList();
	}

	private static void registerSample(
			Map<String, Integer> samples,
			String name,
			List<OpenAIBenchmarkRun> runs,
			Function<OpenAIBenchmarkRun, Long> extractor
	) {
		samples.put(name, knownLongs(runs, extractor).size());
	}

	private static BigDecimal average(List<BigDecimal> values) {
		return values.isEmpty()
				? null
				: sum(values).divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP).stripTrailingZeros();
	}

	private static BigDecimal sum(List<BigDecimal> values) {
		return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static BigDecimal rate(long numerator, long denominator) {
		return denominator == 0
				? null
				: BigDecimal.valueOf(numerator)
						.divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
	}

	private static Long percentile(List<Long> source, double quantile) {
		if (source.isEmpty()) {
			return null;
		}
		List<Long> values = new ArrayList<>(source);
		values.sort(Comparator.naturalOrder());
		int rank = Math.max(1, (int) Math.ceil(quantile * values.size()));
		return values.get(rank - 1);
	}

	private static <T> Map<String, List<T>> orderedGroups(List<T> values, Function<T, String> classifier) {
		Map<String, List<T>> groups = new LinkedHashMap<>();
		for (T value : values) {
			groups.computeIfAbsent(classifier.apply(value), ignored -> new ArrayList<>()).add(value);
		}
		return groups;
	}
}
