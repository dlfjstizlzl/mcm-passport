package com.mcm.passport.domain.demo.benchmark;

import com.mcm.passport.domain.demo.benchmark.OpenAIBenchmarkPricingSnapshot.ModelPrice;
import com.mcm.passport.domain.style.analysis.metrics.OpenAIUsageMetrics;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public final class OpenAIEstimatedCostCalculator {

	private static final int TOKENS_PER_MILLION_SCALE = 6;
	private static final long MAX_STANDARD_SHORT_CONTEXT_INPUT_TOKENS = 272_000L;

	public Optional<BigDecimal> calculateUsd(
			OpenAIUsageMetrics usage,
			String model,
			OpenAIBenchmarkPricingSnapshot pricingSnapshot
	) {
		Objects.requireNonNull(pricingSnapshot, "pricingSnapshot must not be null");
		return pricingSnapshot.findModel(model)
				.flatMap(price -> calculateUsd(usage, price));
	}

	public Optional<BigDecimal> calculateUsd(OpenAIUsageMetrics usage, ModelPrice price) {
		Objects.requireNonNull(usage, "usage must not be null");
		Objects.requireNonNull(price, "price must not be null");
		validateUsage(usage);

		Long inputTokens = usage.inputTokens();
		Long cachedInputTokens = usage.cachedInputTokens();
		Long cacheWriteTokens = usage.cacheWriteTokens();
		Long outputTokens = usage.outputTokens();

		if (inputTokens == null
				|| cachedInputTokens == null
				|| cacheWriteTokens == null
				|| outputTokens == null
				|| inputTokens > MAX_STANDARD_SHORT_CONTEXT_INPUT_TOKENS
				|| price.standardInputPricePerMillion() == null
				|| price.cachedInputPricePerMillion() == null
				|| price.cacheWritePricePerMillion() == null
				|| price.outputPricePerMillion() == null) {
			return Optional.empty();
		}

		long uncachedInputTokens = inputTokens - cachedInputTokens - cacheWriteTokens;
		BigDecimal estimatedCost = tokenCost(uncachedInputTokens, price.standardInputPricePerMillion())
				.add(tokenCost(cachedInputTokens, price.cachedInputPricePerMillion()))
				.add(tokenCost(cacheWriteTokens, price.cacheWritePricePerMillion()))
				.add(tokenCost(outputTokens, price.outputPricePerMillion()));
		return Optional.of(estimatedCost);
	}

	/**
	 * Reserves a conservative short-context amount before a paid request. All input tokens are
	 * priced at the most expensive input category because cache read/write status is not known yet.
	 */
	public Optional<BigDecimal> calculateReservationUsd(
			long inputTokenUpperBound,
			long outputTokenUpperBound,
			String model,
			OpenAIBenchmarkPricingSnapshot pricingSnapshot
	) {
		if (inputTokenUpperBound <= 0 || outputTokenUpperBound <= 0) {
			throw new IllegalArgumentException("reservation token bounds must be positive");
		}
		if (inputTokenUpperBound > MAX_STANDARD_SHORT_CONTEXT_INPUT_TOKENS) {
			return Optional.empty();
		}
		return pricingSnapshot.findModel(model).flatMap(price -> {
			if (price.standardInputPricePerMillion() == null
					|| price.cachedInputPricePerMillion() == null
					|| price.cacheWritePricePerMillion() == null
					|| price.outputPricePerMillion() == null) {
				return Optional.empty();
			}
			BigDecimal maximumInputPrice = price.standardInputPricePerMillion()
					.max(price.cachedInputPricePerMillion())
					.max(price.cacheWritePricePerMillion());
			return Optional.of(
					tokenCost(inputTokenUpperBound, maximumInputPrice)
							.add(tokenCost(outputTokenUpperBound, price.outputPricePerMillion()))
			);
		});
	}

	private void validateUsage(OpenAIUsageMetrics usage) {
		rejectNegative(usage.inputTokens(), "inputTokens");
		rejectNegative(usage.cachedInputTokens(), "cachedInputTokens");
		rejectNegative(usage.cacheWriteTokens(), "cacheWriteTokens");
		rejectNegative(usage.outputTokens(), "outputTokens");
		rejectNegative(usage.reasoningTokens(), "reasoningTokens");
		rejectNegative(usage.totalTokens(), "totalTokens");

		Long inputTokens = usage.inputTokens();
		Long cachedInputTokens = usage.cachedInputTokens();
		Long cacheWriteTokens = usage.cacheWriteTokens();
		if (inputTokens != null && cachedInputTokens != null && cachedInputTokens > inputTokens) {
			throw new IllegalArgumentException("cachedInputTokens must be included in inputTokens");
		}
		if (inputTokens != null && cacheWriteTokens != null && cacheWriteTokens > inputTokens) {
			throw new IllegalArgumentException("cacheWriteTokens must be included in inputTokens");
		}
		if (inputTokens != null && cachedInputTokens != null && cacheWriteTokens != null
				&& cacheWriteTokens > inputTokens - cachedInputTokens) {
			throw new IllegalArgumentException(
					"cachedInputTokens and cacheWriteTokens must be included in inputTokens"
			);
		}

		Long outputTokens = usage.outputTokens();
		Long reasoningTokens = usage.reasoningTokens();
		if (outputTokens != null && reasoningTokens != null && reasoningTokens > outputTokens) {
			throw new IllegalArgumentException("reasoningTokens must be included in outputTokens");
		}

		Long totalTokens = usage.totalTokens();
		if (inputTokens != null && outputTokens != null && totalTokens != null) {
			long expectedTotal;
			try {
				expectedTotal = Math.addExact(inputTokens, outputTokens);
			}
			catch (ArithmeticException exception) {
				throw new IllegalArgumentException("inputTokens and outputTokens exceed the supported total", exception);
			}
			if (totalTokens != expectedTotal) {
				throw new IllegalArgumentException("totalTokens must equal inputTokens plus outputTokens");
			}
		}
	}

	private void rejectNegative(Long value, String field) {
		if (value != null && value < 0) {
			throw new IllegalArgumentException(field + " must not be negative");
		}
	}

	private BigDecimal tokenCost(long tokens, BigDecimal pricePerMillionTokens) {
		return BigDecimal.valueOf(tokens)
				.multiply(pricePerMillionTokens)
				.movePointLeft(TOKENS_PER_MILLION_SCALE);
	}
}
