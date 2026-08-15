package com.mcm.passport.domain.demo.benchmark;

import com.mcm.passport.domain.style.analysis.OpenAIStyleAnalysisProvider;
import com.mcm.passport.domain.style.analysis.RuleBasedStyleFallback;
import com.mcm.passport.domain.style.analysis.StyleAnalysisValidator;
import com.mcm.passport.domain.style.analysis.openai.OpenAIResponsesStyleAnalysisGateway;
import com.mcm.passport.domain.style.analysis.openai.StyleAnalysisPromptFactory;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import java.io.IOException;
import java.io.PrintStream;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Explicit, non-web entry point for the paid OpenAI benchmark.
 *
 * <p>No client is created unless both live flags are true and an API key is configured.</p>
 */
public final class OpenAIBenchmarkMain {

	private static final String API_KEY_ENV = "OPENAI_API_KEY";
	private static final String TIMEOUT_ENV = "OPENAI_TIMEOUT";
	private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

	private OpenAIBenchmarkMain() {
	}

	public static void main(String[] args) {
		int exitCode = run(System.getenv(), System.out);
		if (exitCode != 0) {
			System.exit(exitCode);
		}
	}

	/** Visible for a network-free preflight test. */
	public static int run(Map<String, String> environment, PrintStream output) {
		Objects.requireNonNull(environment, "environment must not be null");
		Objects.requireNonNull(output, "output must not be null");

		OpenAIBenchmarkConfig config;
		try {
			config = OpenAIBenchmarkConfig.fromEnvironment(environment);
		}
		catch (RuntimeException invalidConfiguration) {
			output.println("Benchmark status: NOT_RUN");
			output.println("Configuration error: " + invalidConfiguration.getClass().getSimpleName());
			return 2;
		}

		String apiKey = environment.get(API_KEY_ENV);
		boolean apiKeyConfigured = apiKey != null && !apiKey.isBlank();
		new OpenAIBenchmarkPreflight().print(config, apiKeyConfigured, output);

		if (!config.enabled()) {
			output.println("Benchmark status: NOT_RUN");
			return 0;
		}
		if (!config.confirmLive()) {
			output.println("Benchmark status: PENDING_LOCAL_LIVE_RUN");
			output.println("No request was sent because live confirmation is disabled.");
			return 0;
		}
		if (!apiKeyConfigured) {
			output.println("Benchmark status: PENDING_LOCAL_LIVE_RUN");
			output.println("No request was sent because the OpenAI API key is not configured.");
			return 0;
		}

		try {
			OpenAIClient client = OpenAIOkHttpClient.builder()
					.apiKey(apiKey.trim())
					.timeout(parseTimeout(environment.get(TIMEOUT_ENV)))
					.maxRetries(0)
					.build();
			OpenAIStyleAnalysisProvider provider = new OpenAIStyleAnalysisProvider(
					new OpenAIResponsesStyleAnalysisGateway(client),
					new StyleAnalysisPromptFactory(),
					config.models().getFirst(),
					config.reasoningEffort().value()
			);
			OpenAIBenchmarkPricingSnapshot pricing = OpenAIBenchmarkPricingSnapshot.standardShortContext();
			OpenAIBenchmarkRunner runner = new OpenAIBenchmarkRunner(
					provider,
					new StyleAnalysisValidator(),
					new RuleBasedStyleFallback(),
					new OpenAIEstimatedCostCalculator(),
					pricing,
					new OpenAIBenchmarkErrorClassifier(),
					Clock.systemUTC(),
					System::nanoTime,
					output
			);

			OpenAIBenchmarkExecution execution = runner.run(config);
			OpenAIBenchmarkSummary summary = OpenAIBenchmarkSummary.from(execution, pricing);
			List<java.nio.file.Path> reports = new OpenAIBenchmarkReportWriter().write(
					config.reportDirectory(),
					execution,
					summary
			);
			output.println("Benchmark status: " + summary.status());
			output.println("Termination reason: " + summary.terminationReason());
			reports.forEach(path -> output.println("Report: " + path));
			return 0;
		}
		catch (IOException reportFailure) {
			output.println("Benchmark status: REPORT_WRITE_FAILED");
			output.println("Failure type: " + reportFailure.getClass().getSimpleName());
			return 3;
		}
		catch (RuntimeException benchmarkFailure) {
			output.println("Benchmark status: STOPPED");
			output.println("Failure type: " + benchmarkFailure.getClass().getSimpleName());
			return 4;
		}
	}

	private static Duration parseTimeout(String raw) {
		if (raw == null || raw.isBlank()) {
			return DEFAULT_TIMEOUT;
		}
		String value = raw.trim().toLowerCase(Locale.ROOT);
		try {
			Duration parsed;
			if (value.startsWith("p")) {
				parsed = Duration.parse(value.toUpperCase(Locale.ROOT));
			}
			else if (value.endsWith("ms")) {
				parsed = Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
			}
			else if (value.endsWith("s")) {
				parsed = Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1)));
			}
			else if (value.endsWith("m")) {
				parsed = Duration.ofMinutes(Long.parseLong(value.substring(0, value.length() - 1)));
			}
			else if (value.endsWith("h")) {
				parsed = Duration.ofHours(Long.parseLong(value.substring(0, value.length() - 1)));
			}
			else {
				parsed = Duration.ofMillis(Long.parseLong(value));
			}
			if (parsed.isZero() || parsed.isNegative()) {
				throw new IllegalArgumentException("timeout must be positive");
			}
			return parsed;
		}
		catch (RuntimeException invalidTimeout) {
			throw new IllegalArgumentException("OPENAI_TIMEOUT must be a positive duration", invalidTimeout);
		}
	}
}
