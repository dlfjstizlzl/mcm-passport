package com.mcm.passport.domain.demo.benchmark;

import com.mcm.passport.domain.style.analysis.openai.OpenAIReasoningEffort;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAIBenchmarkConfigAndPreflightTest {

	@Test
	void defaultsToAnExplicitlyDisabledThirtyCallNoneReasoningPlan() {
		OpenAIBenchmarkConfig config = OpenAIBenchmarkConfig.fromEnvironment(Map.of());

		assertThat(config.enabled()).isFalse();
		assertThat(config.confirmLive()).isFalse();
		assertThat(config.models()).containsExactly(
				"gpt-5.6-luna",
				"gpt-5.6-terra",
				"gpt-5.6-sol"
		);
		assertThat(config.repetitions()).isEqualTo(5);
		assertThat(config.plannedCalls()).isEqualTo(30L);
		assertThat(config.maxCalls()).isEqualTo(30);
		assertThat(config.maxEstimatedUsd()).isEqualByComparingTo("1.00");
		assertThat(config.reasoningEffort()).isEqualTo(OpenAIReasoningEffort.NONE);
		assertThat(config.liveAuthorized(true)).isFalse();
	}

	@Test
	void parsesLocalOverridesWithoutDependingOnAnApiKey() {
		OpenAIBenchmarkConfig config = OpenAIBenchmarkConfig.fromEnvironment(Map.of(
				OpenAIBenchmarkConfig.ENABLED_ENV, "true",
				OpenAIBenchmarkConfig.CONFIRM_LIVE_ENV, "true",
				OpenAIBenchmarkConfig.MODELS_ENV, "gpt-5.6-terra, gpt-5.6-luna",
				OpenAIBenchmarkConfig.REPETITIONS_ENV, "2",
				OpenAIBenchmarkConfig.MAX_CALLS_ENV, "9",
				OpenAIBenchmarkConfig.MAX_ESTIMATED_USD_ENV, "0.25",
				OpenAIBenchmarkConfig.REASONING_EFFORT_ENV, "low",
				OpenAIBenchmarkConfig.REPORT_DIRECTORY_ENV, "build/custom-benchmark"
		));

		assertThat(config.models()).containsExactly("gpt-5.6-terra", "gpt-5.6-luna");
		assertThat(config.repetitions()).isEqualTo(2);
		assertThat(config.plannedCalls()).isEqualTo(8L);
		assertThat(config.maxCalls()).isEqualTo(9);
		assertThat(config.maxEstimatedUsd()).isEqualByComparingTo("0.25");
		assertThat(config.reasoningEffort()).isEqualTo(OpenAIReasoningEffort.LOW);
		assertThat(config.reportDirectory()).isEqualTo(Path.of("build/custom-benchmark"));
		assertThat(config.liveAuthorized(false)).isFalse();
		assertThat(config.liveAuthorized(true)).isTrue();
	}

	@Test
	void preflightPrintsThePlanAndOnlyReportsWhetherAKeyExists() {
		OpenAIBenchmarkConfig config = new OpenAIBenchmarkConfig(
				true,
				true,
				OpenAIBenchmarkConfig.DEFAULT_MODELS,
				5,
				30,
				new BigDecimal("1.00"),
				OpenAIReasoningEffort.NONE,
				Path.of("build/reports/openai-benchmark")
		);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();

		new OpenAIBenchmarkPreflight().print(
				config,
				true,
				new PrintStream(bytes, true, StandardCharsets.UTF_8)
		);
		String output = bytes.toString(StandardCharsets.UTF_8);

		assertThat(output)
				.contains("OpenAI API key configured: yes")
				.contains("Benchmark enabled: yes")
				.contains("Live confirmation: yes")
				.contains("gpt-5.6-luna", "CASE_A_WITHOUT_PRODUCT_TAG", "CASE_B_WITH_PRODUCT_TAG")
				.contains("Repetitions: 5", "Reasoning effort: none", "Planned calls: 30")
				.contains("Maximum calls: 30", "Maximum estimated USD: 1.00")
				.doesNotContain("OPENAI_API_KEY", "sk-");
	}

	@Test
	void mainPreflightNeverCreatesALiveClientUnlessBothFlagsAndAKeyArePresent() {
		String fakeSecret = "offline-secret-sentinel";
		RunResult disabled = runMain(Map.of("OPENAI_API_KEY", fakeSecret));
		RunResult confirmationMissing = runMain(Map.of(
				"OPENAI_API_KEY", fakeSecret,
				OpenAIBenchmarkConfig.ENABLED_ENV, "true",
				OpenAIBenchmarkConfig.CONFIRM_LIVE_ENV, "false"
		));
		RunResult keyMissing = runMain(Map.of(
				OpenAIBenchmarkConfig.ENABLED_ENV, "true",
				OpenAIBenchmarkConfig.CONFIRM_LIVE_ENV, "true"
		));

		assertThat(disabled.exitCode()).isZero();
		assertThat(disabled.output()).contains("Benchmark status: NOT_RUN").doesNotContain(fakeSecret);
		assertThat(confirmationMissing.exitCode()).isZero();
		assertThat(confirmationMissing.output())
				.contains("PENDING_LOCAL_LIVE_RUN", "live confirmation is disabled")
				.doesNotContain(fakeSecret);
		assertThat(keyMissing.exitCode()).isZero();
		assertThat(keyMissing.output())
				.contains("PENDING_LOCAL_LIVE_RUN", "API key is not configured")
				.doesNotContain(fakeSecret);
	}

	private RunResult runMain(Map<String, String> environment) {
		Map<String, String> isolatedEnvironment = new HashMap<>(environment);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		int exitCode = OpenAIBenchmarkMain.run(
				isolatedEnvironment,
				new PrintStream(bytes, true, StandardCharsets.UTF_8)
		);
		return new RunResult(exitCode, bytes.toString(StandardCharsets.UTF_8));
	}

	private record RunResult(int exitCode, String output) {
	}
}
