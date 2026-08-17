package com.mcm.passport.domain.demo.benchmark;

import com.mcm.passport.domain.style.analysis.openai.OpenAIReasoningEffort;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record OpenAIBenchmarkConfig(
		boolean enabled,
		boolean confirmLive,
		List<String> models,
		List<String> caseNames,
		int repetitions,
		int maxCalls,
		long maxOutputTokens,
		BigDecimal maxEstimatedUsd,
		OpenAIReasoningEffort reasoningEffort,
		Path reportDirectory
) {

	public static final String ENABLED_ENV = "MCM_OPENAI_BENCHMARK";
	public static final String CONFIRM_LIVE_ENV = "MCM_OPENAI_BENCHMARK_CONFIRM_LIVE";
	public static final String MODELS_ENV = "MCM_OPENAI_BENCHMARK_MODELS";
	public static final String CASES_ENV = "MCM_OPENAI_BENCHMARK_CASES";
	public static final String REPETITIONS_ENV = "MCM_OPENAI_BENCHMARK_REPETITIONS";
	public static final String MAX_CALLS_ENV = "MCM_OPENAI_BENCHMARK_MAX_CALLS";
	public static final String MAX_OUTPUT_TOKENS_ENV = "MCM_OPENAI_BENCHMARK_MAX_OUTPUT_TOKENS";
	public static final String MAX_ESTIMATED_USD_ENV = "MCM_OPENAI_BENCHMARK_MAX_ESTIMATED_USD";
	public static final String REASONING_EFFORT_ENV = "MCM_OPENAI_REASONING_EFFORT";
	public static final String REPORT_DIRECTORY_ENV = "MCM_OPENAI_BENCHMARK_REPORT_DIR";
	public static final long DEFAULT_MAX_OUTPUT_TOKENS = 512L;
	public static final long MAX_OUTPUT_TOKENS_HARD_CAP = 25_000L;

	public static final List<String> DEFAULT_MODELS = List.of(
			"gpt-5.6-luna",
			"gpt-5.6-terra",
			"gpt-5.6-sol"
	);

	public OpenAIBenchmarkConfig {
		models = List.copyOf(Objects.requireNonNull(models, "models must not be null"));
		if (models.isEmpty() || models.stream().anyMatch(model -> model == null || model.isBlank())) {
			throw new IllegalArgumentException("at least one benchmark model is required");
		}
		if (!DEFAULT_MODELS.containsAll(models)) {
			throw new IllegalArgumentException("benchmark models must be GPT-5.6 Luna, Terra, or Sol");
		}
		caseNames = deduplicate(Objects.requireNonNull(caseNames, "caseNames must not be null"));
		if (caseNames.isEmpty()) {
			throw new IllegalArgumentException("at least one benchmark case is required");
		}
		if (!OpenAIBenchmarkFixtures.caseNames().containsAll(caseNames)) {
			throw new IllegalArgumentException("benchmark cases must use exact known fixture names");
		}
		if (repetitions <= 0) {
			throw new IllegalArgumentException("benchmark repetitions must be positive");
		}
		if (maxCalls <= 0) {
			throw new IllegalArgumentException("benchmark max calls must be positive");
		}
		if (maxOutputTokens <= 0 || maxOutputTokens > MAX_OUTPUT_TOKENS_HARD_CAP) {
			throw new IllegalArgumentException(
					"benchmark maximum output tokens must be between 1 and " + MAX_OUTPUT_TOKENS_HARD_CAP
			);
		}
		Objects.requireNonNull(maxEstimatedUsd, "maxEstimatedUsd must not be null");
		if (maxEstimatedUsd.signum() <= 0) {
			throw new IllegalArgumentException("benchmark maximum estimated USD must be positive");
		}
		Objects.requireNonNull(reasoningEffort, "reasoningEffort must not be null");
		Objects.requireNonNull(reportDirectory, "reportDirectory must not be null");
	}

	public OpenAIBenchmarkConfig(
			boolean enabled,
			boolean confirmLive,
			List<String> models,
			int repetitions,
			int maxCalls,
			BigDecimal maxEstimatedUsd,
			OpenAIReasoningEffort reasoningEffort,
			Path reportDirectory
	) {
		this(
				enabled,
				confirmLive,
				models,
				OpenAIBenchmarkFixtures.caseNames(),
				repetitions,
				maxCalls,
				DEFAULT_MAX_OUTPUT_TOKENS,
				maxEstimatedUsd,
				reasoningEffort,
				reportDirectory
		);
	}

	public static OpenAIBenchmarkConfig fromEnvironment(Map<String, String> environment) {
		Objects.requireNonNull(environment, "environment must not be null");
		return new OpenAIBenchmarkConfig(
				parseBoolean(environment, ENABLED_ENV, false),
				parseBoolean(environment, CONFIRM_LIVE_ENV, false),
				parseModels(environment.get(MODELS_ENV)),
				parseCases(environment.get(CASES_ENV)),
				parsePositiveInt(environment, REPETITIONS_ENV, 5),
				parsePositiveInt(environment, MAX_CALLS_ENV, 30),
				parseMaxOutputTokens(environment.get(MAX_OUTPUT_TOKENS_ENV)),
				parsePositiveDecimal(environment, MAX_ESTIMATED_USD_ENV, new BigDecimal("1.00")),
				OpenAIReasoningEffort.parse(valueOrDefault(environment, REASONING_EFFORT_ENV, "none")),
				Path.of(valueOrDefault(
						environment,
						REPORT_DIRECTORY_ENV,
						"build/reports/openai-benchmark"
				))
		);
	}

	public long plannedCalls() {
		return Math.multiplyExact(
				Math.multiplyExact((long) models.size(), caseNames.size()),
				repetitions
		);
	}

	public boolean liveAuthorized(boolean apiKeyConfigured) {
		return enabled && confirmLive && apiKeyConfigured;
	}

	private static List<String> parseModels(String raw) {
		if (raw == null || raw.isBlank()) {
			return DEFAULT_MODELS;
		}
		LinkedHashSet<String> models = new LinkedHashSet<>();
		Arrays.stream(raw.split(","))
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.forEach(models::add);
		return List.copyOf(models);
	}

	private static List<String> parseCases(String raw) {
		if (raw == null || raw.isBlank()) {
			return OpenAIBenchmarkFixtures.caseNames();
		}
		LinkedHashSet<String> cases = new LinkedHashSet<>();
		Arrays.stream(raw.split(","))
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.forEach(cases::add);
		return List.copyOf(cases);
	}

	private static long parseMaxOutputTokens(String raw) {
		if (raw == null || raw.isBlank()) {
			return DEFAULT_MAX_OUTPUT_TOKENS;
		}
		try {
			return Long.parseLong(raw.trim());
		}
		catch (NumberFormatException exception) {
			throw new IllegalArgumentException(
					MAX_OUTPUT_TOKENS_ENV + " must be a positive integer no greater than "
							+ MAX_OUTPUT_TOKENS_HARD_CAP,
					exception
			);
		}
	}

	private static List<String> deduplicate(List<String> values) {
		return List.copyOf(new LinkedHashSet<>(values));
	}

	private static boolean parseBoolean(Map<String, String> environment, String name, boolean defaultValue) {
		String value = environment.get(name);
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		return switch (value.trim().toLowerCase(Locale.ROOT)) {
			case "true" -> true;
			case "false" -> false;
			default -> throw new IllegalArgumentException(name + " must be true or false");
		};
	}

	private static int parsePositiveInt(Map<String, String> environment, String name, int defaultValue) {
		String value = environment.get(name);
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		try {
			int parsed = Integer.parseInt(value.trim());
			if (parsed <= 0) {
				throw new IllegalArgumentException(name + " must be positive");
			}
			return parsed;
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(name + " must be a positive integer", exception);
		}
	}

	private static BigDecimal parsePositiveDecimal(
			Map<String, String> environment,
			String name,
			BigDecimal defaultValue
	) {
		String value = environment.get(name);
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		try {
			BigDecimal parsed = new BigDecimal(value.trim());
			if (parsed.signum() <= 0) {
				throw new IllegalArgumentException(name + " must be positive");
			}
			return parsed;
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(name + " must be a positive decimal", exception);
		}
	}

	private static String valueOrDefault(Map<String, String> environment, String name, String defaultValue) {
		String value = environment.get(name);
		return value == null || value.isBlank() ? defaultValue : value.trim();
	}
}
