package com.mcm.passport.domain.demo.benchmark;

import com.mcm.passport.domain.style.analysis.OpenAIStyleAnalysisProvider;
import com.mcm.passport.domain.style.analysis.RuleBasedStyleFallback;
import com.mcm.passport.domain.style.analysis.StyleAnalysisCandidate;
import com.mcm.passport.domain.style.analysis.StyleAnalysisValidator;
import com.mcm.passport.domain.style.analysis.ValidatedStyleAnalysis;
import com.mcm.passport.domain.style.analysis.metrics.OpenAIStyleAnalysisMeasurement;
import com.mcm.passport.domain.style.analysis.metrics.OpenAIUsageMetrics;
import com.mcm.passport.domain.style.analysis.openai.OpenAIMeteredException;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisGatewayRequest;
import com.mcm.passport.domain.style.analysis.openai.StyleAnalysisPrompt;
import com.mcm.passport.domain.style.analysis.openai.StyleAnalysisPromptFactory;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class OpenAIBenchmarkRunner {

	static final long RESERVED_INPUT_TOKENS = 8_192L;
	static final long MAX_RENDERED_PROMPT_UTF8_BYTES = 4_096L;
	static final long MAX_OUTPUT_TOKENS = 512L;

	private final OpenAIStyleAnalysisProvider provider;
	private final StyleAnalysisValidator validator;
	private final RuleBasedStyleFallback fallback;
	private final OpenAIEstimatedCostCalculator costCalculator;
	private final OpenAIBenchmarkPricingSnapshot pricing;
	private final OpenAIBenchmarkErrorClassifier errorClassifier;
	private final Clock clock;
	private final LongSupplier nanoTime;
	private final PrintStream output;

	public OpenAIBenchmarkRunner(
			OpenAIStyleAnalysisProvider provider,
			StyleAnalysisValidator validator,
			RuleBasedStyleFallback fallback,
			OpenAIEstimatedCostCalculator costCalculator,
			OpenAIBenchmarkPricingSnapshot pricing,
			OpenAIBenchmarkErrorClassifier errorClassifier,
			Clock clock,
			LongSupplier nanoTime,
			PrintStream output
	) {
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.validator = Objects.requireNonNull(validator, "validator must not be null");
		this.fallback = Objects.requireNonNull(fallback, "fallback must not be null");
		this.costCalculator = Objects.requireNonNull(costCalculator, "costCalculator must not be null");
		this.pricing = Objects.requireNonNull(pricing, "pricing must not be null");
		this.errorClassifier = Objects.requireNonNull(errorClassifier, "errorClassifier must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime must not be null");
		this.output = Objects.requireNonNull(output, "output must not be null");
	}

	public OpenAIBenchmarkExecution run(OpenAIBenchmarkConfig config) {
		Objects.requireNonNull(config, "config must not be null");
		String startedAt = Instant.now(clock).toString();
		List<OpenAIBenchmarkRun> runs = new ArrayList<>();
		BigDecimal accumulatedEstimatedCost = BigDecimal.ZERO;
		OpenAIBenchmarkErrorCategory terminationReason = OpenAIBenchmarkErrorCategory.NONE;
		int attemptedCalls = 0;

		models:
		for (String model : config.models()) {
			boolean modelUnavailable = false;
			for (OpenAIBenchmarkCase benchmarkCase : OpenAIBenchmarkFixtures.cases()) {
				for (int repetition = 0; repetition < config.repetitions(); repetition++) {
					if (attemptedCalls >= config.maxCalls()) {
						terminationReason = OpenAIBenchmarkErrorCategory.CALL_CAP_REACHED;
						break models;
					}
					if (renderedPromptUtf8Bytes(benchmarkCase) > MAX_RENDERED_PROMPT_UTF8_BYTES) {
						terminationReason = OpenAIBenchmarkErrorCategory.COST_ESTIMATE_UNAVAILABLE;
						break models;
					}
					Optional<BigDecimal> reservation = costCalculator.calculateReservationUsd(
							RESERVED_INPUT_TOKENS,
							MAX_OUTPUT_TOKENS,
							model,
							pricing
					);
					if (reservation.isEmpty()) {
						terminationReason = OpenAIBenchmarkErrorCategory.COST_ESTIMATE_UNAVAILABLE;
						break models;
					}
					if (wouldReachCostCap(
							accumulatedEstimatedCost,
							reservation.orElseThrow(),
							config.maxEstimatedUsd()
					)) {
						terminationReason = OpenAIBenchmarkErrorCategory.COST_CAP_REACHED;
						break models;
					}

					attemptedCalls++;
					OpenAIBenchmarkRun run = executeOne(
							attemptedCalls,
							benchmarkCase,
							new OpenAIStyleAnalysisGatewayRequest(
									model,
									config.reasoningEffort(),
									MAX_OUTPUT_TOKENS
							)
					);
					runs.add(run);
					logSafely(run);

					if (run.errorCategory() == OpenAIBenchmarkErrorCategory.AUTHENTICATION) {
						terminationReason = OpenAIBenchmarkErrorCategory.AUTHENTICATION;
						break models;
					}
					if (run.errorCategory() == OpenAIBenchmarkErrorCategory.MODEL_UNAVAILABLE) {
						modelUnavailable = true;
						break;
					}
					if (run.estimatedCostUsd() != null) {
						accumulatedEstimatedCost = accumulatedEstimatedCost.add(run.estimatedCostUsd());
						if (accumulatedEstimatedCost.compareTo(config.maxEstimatedUsd()) >= 0) {
							terminationReason = OpenAIBenchmarkErrorCategory.COST_CAP_REACHED;
							break models;
						}
					} else if (run.errorCategory() != OpenAIBenchmarkErrorCategory.MODEL_UNAVAILABLE) {
						terminationReason = OpenAIBenchmarkErrorCategory.COST_ESTIMATE_UNAVAILABLE;
						break models;
					}

				}
				if (modelUnavailable) {
					break;
				}
			}
		}

		return new OpenAIBenchmarkExecution(
				startedAt,
				Instant.now(clock).toString(),
				terminationReason,
				runs
		);
	}

	static long renderedPromptUtf8Bytes(OpenAIBenchmarkCase benchmarkCase) {
		StyleAnalysisPrompt prompt = new StyleAnalysisPromptFactory().create(benchmarkCase.journeyData());
		return Math.addExact(
				prompt.instructions().getBytes(StandardCharsets.UTF_8).length,
				prompt.input().getBytes(StandardCharsets.UTF_8).length
		);
	}

	private OpenAIBenchmarkRun executeOne(
			int runNumber,
			OpenAIBenchmarkCase benchmarkCase,
			OpenAIStyleAnalysisGatewayRequest request
	) {
		long endToEndStartedAt = nanoTime.getAsLong();
		OpenAIStyleAnalysisMeasurement measurement = null;
		OpenAIUsageMetrics failureUsage = OpenAIUsageMetrics.allUnavailable();
		Long failureProviderLatencyMs = null;
		ValidatedStyleAnalysis analysis;
		boolean usedFallback = false;
		boolean success = false;
		OpenAIBenchmarkErrorCategory errorCategory = OpenAIBenchmarkErrorCategory.NONE;

		try {
			measurement = provider.analyzeWithMetrics(benchmarkCase.journeyData(), request);
			analysis = validator.validate(measurement.candidate());
			success = true;
		} catch (RuntimeException providerFailure) {
			OpenAIMeteredException meteredFailure = findMeteredFailure(providerFailure);
			if (meteredFailure != null) {
				failureUsage = meteredFailure.usage();
				failureProviderLatencyMs = meteredFailure.providerLatencyMs();
			}
			usedFallback = true;
			errorCategory = errorClassifier.classify(providerFailure);
			if (errorCategory == OpenAIBenchmarkErrorCategory.UNKNOWN) {
				errorCategory = OpenAIBenchmarkErrorCategory.FALLBACK;
			}
			try {
				StyleAnalysisCandidate fallbackCandidate = fallback.analyze(benchmarkCase.journeyData());
				analysis = validator.validate(fallbackCandidate);
			} catch (RuntimeException fallbackFailure) {
				analysis = null;
				errorCategory = OpenAIBenchmarkErrorCategory.FALLBACK;
			}
		}

		OpenAIUsageMetrics usage = measurement == null
				? failureUsage
				: measurement.usage();
		BigDecimal estimatedCost = null;
		try {
			Optional<BigDecimal> calculatedCost = costCalculator.calculateUsd(
					usage,
					request.model(),
					pricing
			);
			estimatedCost = calculatedCost.orElse(null);
			if (calculatedCost.isEmpty() && errorCategory == OpenAIBenchmarkErrorCategory.NONE) {
				errorCategory = OpenAIBenchmarkErrorCategory.COST_ESTIMATE_UNAVAILABLE;
				success = false;
			}
		} catch (IllegalArgumentException invalidUsage) {
			errorCategory = OpenAIBenchmarkErrorCategory.COST_ESTIMATE_UNAVAILABLE;
			success = false;
		}

		long endToEndLatencyMs = elapsedMillis(endToEndStartedAt, nanoTime.getAsLong());
		return new OpenAIBenchmarkRun(
				runNumber,
				benchmarkCase.name(),
				request.model(),
				request.effort().value(),
				analysis == null ? null : analysis.cityCode().name(),
				analysis == null ? null : analysis.recommendedProduct().name(),
				analysis == null ? null : analysis.styleMood().name(),
				analysis == null ? null : analysis.background().name(),
				analysis == null ? null : analysis.matchScore(),
				usedFallback,
				usage.inputTokens(),
				usage.cachedInputTokens(),
				usage.cacheWriteTokens(),
				usage.outputTokens(),
				usage.reasoningTokens(),
				usage.totalTokens(),
				measurement == null
						? failureProviderLatencyMs
						: Long.valueOf(measurement.providerLatencyMs()),
				endToEndLatencyMs,
				estimatedCost,
				success && !usedFallback && errorCategory == OpenAIBenchmarkErrorCategory.NONE,
				errorCategory
		);
	}

	private boolean wouldReachCostCap(
			BigDecimal accumulated,
			BigDecimal reservedCallCost,
			BigDecimal cap
	) {
		return accumulated.compareTo(cap) >= 0
				|| accumulated.add(reservedCallCost).compareTo(cap) > 0;
	}

	private OpenAIMeteredException findMeteredFailure(Throwable failure) {
		Throwable current = failure;
		for (int depth = 0; current != null && depth < 16; depth++) {
			if (current instanceof OpenAIMeteredException meteredFailure) {
				return meteredFailure;
			}
			current = current.getCause();
		}
		return null;
	}

	private long elapsedMillis(long startedAt, long completedAt) {
		return TimeUnit.NANOSECONDS.toMillis(Math.max(0L, completedAt - startedAt));
	}

	private void logSafely(OpenAIBenchmarkRun run) {
		output.printf(
				"Benchmark run %d: model=%s, inputTokens=%s, cachedInputTokens=%s, "
						+ "cacheWriteTokens=%s, outputTokens=%s, reasoningTokens=%s, totalTokens=%s, "
						+ "providerLatencyMs=%s, endToEndLatencyMs=%d, usedFallback=%s, errorCategory=%s%n",
				run.runNumber(),
				run.model(),
				value(run.inputTokens()),
				value(run.cachedInputTokens()),
				value(run.cacheWriteTokens()),
				value(run.outputTokens()),
				value(run.reasoningTokens()),
				value(run.totalTokens()),
				value(run.providerLatencyMs()),
				run.endToEndLatencyMs(),
				run.usedFallback(),
				run.errorCategory()
		);
	}

	private String value(Object value) {
		return Optional.ofNullable(value).map(Object::toString).orElse("UNAVAILABLE");
	}
}
