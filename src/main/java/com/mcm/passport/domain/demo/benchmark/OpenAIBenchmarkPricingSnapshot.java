package com.mcm.passport.domain.demo.benchmark;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record OpenAIBenchmarkPricingSnapshot(
		LocalDate pricingCheckedAt,
		Map<String, ModelPrice> modelPrices
) {

	private static final OpenAIBenchmarkPricingSnapshot STANDARD_SHORT_CONTEXT =
			new OpenAIBenchmarkPricingSnapshot(
					LocalDate.of(2026, 8, 16),
					Map.of(
							"gpt-5.6-sol",
							new ModelPrice(
									new BigDecimal("5.00"),
									new BigDecimal("0.50"),
									new BigDecimal("6.25"),
									new BigDecimal("30.00")
							),
							"gpt-5.6-terra",
							new ModelPrice(
									new BigDecimal("2.00"),
									new BigDecimal("0.20"),
									new BigDecimal("2.50"),
									new BigDecimal("12.00")
							),
							"gpt-5.6-luna",
							new ModelPrice(
									new BigDecimal("0.20"),
									new BigDecimal("0.02"),
									new BigDecimal("0.25"),
									new BigDecimal("1.20")
							)
					)
			);

	public OpenAIBenchmarkPricingSnapshot {
		Objects.requireNonNull(pricingCheckedAt, "pricingCheckedAt must not be null");
		Objects.requireNonNull(modelPrices, "modelPrices must not be null");
		modelPrices.forEach((model, price) -> {
			if (model == null || model.isBlank()) {
				throw new IllegalArgumentException("model name must not be blank");
			}
			Objects.requireNonNull(price, "model price must not be null");
		});
		modelPrices = Map.copyOf(modelPrices);
	}

	public static OpenAIBenchmarkPricingSnapshot standardShortContext() {
		return STANDARD_SHORT_CONTEXT;
	}

	public Optional<ModelPrice> findModel(String model) {
		if (model == null || model.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(modelPrices.get(model.trim()));
	}

	public record ModelPrice(
			BigDecimal standardInputPricePerMillion,
			BigDecimal cachedInputPricePerMillion,
			BigDecimal cacheWritePricePerMillion,
			BigDecimal outputPricePerMillion
	) {

		public ModelPrice {
			rejectNegative(standardInputPricePerMillion, "input price");
			rejectNegative(cachedInputPricePerMillion, "cached input price");
			rejectNegative(cacheWritePricePerMillion, "cache write price");
			rejectNegative(outputPricePerMillion, "output price");
		}

		private static void rejectNegative(BigDecimal value, String label) {
			if (value != null && value.signum() < 0) {
				throw new IllegalArgumentException(label + " must not be negative");
			}
		}
	}
}
