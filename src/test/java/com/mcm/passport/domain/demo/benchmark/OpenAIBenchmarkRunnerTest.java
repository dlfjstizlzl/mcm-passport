package com.mcm.passport.domain.demo.benchmark;

import com.mcm.passport.domain.style.analysis.OpenAIStyleAnalysisProvider;
import com.mcm.passport.domain.style.analysis.RuleBasedStyleFallback;
import com.mcm.passport.domain.style.analysis.StyleAnalysisValidator;
import com.mcm.passport.domain.style.analysis.metrics.OpenAIUsageMetrics;
import com.mcm.passport.domain.style.analysis.openai.OpenAIMeteredException;
import com.mcm.passport.domain.style.analysis.openai.OpenAIReasoningEffort;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisGateway;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisGatewayRequest;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisGatewayResult;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisOutput;
import com.mcm.passport.domain.style.analysis.openai.StyleAnalysisPrompt;
import com.mcm.passport.domain.style.analysis.openai.StyleAnalysisPromptFactory;
import com.mcm.passport.domain.style.entity.CityBackground;
import com.mcm.passport.domain.style.entity.CityCode;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import com.mcm.passport.domain.style.entity.StyleMood;
import com.openai.core.http.Headers;
import com.openai.errors.NotFoundException;
import com.openai.errors.UnauthorizedException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAIBenchmarkRunnerTest {

	private static final OpenAIUsageMetrics COMPLETE_USAGE = new OpenAIUsageMetrics(
			1_000L,
			100L,
			50L,
			100L,
			20L,
			1_100L
	);

	@Test
	void recordsGatewayUsageProviderLatencyAndSeparateEndToEndLatencyOffline() {
		FakeGateway gateway = FakeGateway.succeeding();
		OpenAIBenchmarkExecution execution = runner(gateway).run(config(
				List.of("gpt-5.6-luna"), 1, 1, "1.00"
		));

		assertThat(execution.runs()).singleElement().satisfies(run -> {
			assertThat(run.model()).isEqualTo("gpt-5.6-luna");
			assertThat(run.reasoningEffort()).isEqualTo("none");
			assertThat(run.inputTokens()).isEqualTo(1_000L);
			assertThat(run.cachedInputTokens()).isEqualTo(100L);
			assertThat(run.cacheWriteTokens()).isEqualTo(50L);
			assertThat(run.outputTokens()).isEqualTo(100L);
			assertThat(run.reasoningTokens()).isEqualTo(20L);
			assertThat(run.totalTokens()).isEqualTo(1_100L);
			assertThat(run.providerLatencyMs()).isEqualTo(17L);
			assertThat(run.endToEndLatencyMs()).isEqualTo(5L);
			assertThat(run.success()).isTrue();
			assertThat(run.usedFallback()).isFalse();
		});
		assertThat(gateway.requests()).singleElement().satisfies(request -> {
			assertThat(request.model()).isEqualTo("gpt-5.6-luna");
			assertThat(request.effort()).isEqualTo(OpenAIReasoningEffort.NONE);
			assertThat(request.maxOutputTokens()).isEqualTo(OpenAIBenchmarkRunner.MAX_OUTPUT_TOKENS);
		});
	}

	@Test
	void neverAttemptsMoreThanTheConfiguredMaximumCallCount() {
		FakeGateway gateway = FakeGateway.succeeding();

		OpenAIBenchmarkExecution execution = runner(gateway).run(config(
				List.of("gpt-5.6-luna"), 5, 3, "1.00"
		));

		assertThat(execution.runs()).hasSize(3);
		assertThat(gateway.requests()).hasSize(3);
		assertThat(execution.terminationReason()).isEqualTo(OpenAIBenchmarkErrorCategory.CALL_CAP_REACHED);
	}

	@Test
	void defaultThreeModelTwoCaseFiveRepetitionPlanCompletesThirtyCallsUnderTheGuard() {
		FakeGateway gateway = FakeGateway.succeeding();

		OpenAIBenchmarkExecution execution = runner(gateway).run(config(
				OpenAIBenchmarkConfig.DEFAULT_MODELS, 5, 30, "1.00"
		));

		assertThat(execution.terminationReason()).isEqualTo(OpenAIBenchmarkErrorCategory.NONE);
		assertThat(execution.runs()).hasSize(30);
		assertThat(gateway.requests()).hasSize(30);
		assertThat(execution.runs())
				.extracting(OpenAIBenchmarkRun::model)
				.containsOnly("gpt-5.6-luna", "gpt-5.6-terra", "gpt-5.6-sol");
	}

	@Test
	void stopsBeforeTheFirstCallWhenItsReservedMaximumWouldExceedTheCostCap() {
		FakeGateway gateway = FakeGateway.succeeding();

		OpenAIBenchmarkExecution execution = runner(gateway).run(config(
				List.of("gpt-5.6-luna"), 5, 30, "0.0004"
		));

		assertThat(execution.runs()).isEmpty();
		assertThat(gateway.requests()).isEmpty();
		assertThat(execution.terminationReason()).isEqualTo(OpenAIBenchmarkErrorCategory.COST_CAP_REACHED);
	}

	@Test
	void preservesUsageAndLatencyFromAMeteredFailureWithoutRawResponseData() {
		OpenAIUsageMetrics failureUsage = new OpenAIUsageMetrics(
				900L, 100L, 25L, 80L, 15L, 980L
		);
		FakeGateway gateway = FakeGateway.withBehavior((prompt, request) -> {
			throw new OpenAIMeteredException(
					new IllegalStateException("safe offline failure"),
					failureUsage,
					request.model(),
					44L
			);
		});

		OpenAIBenchmarkExecution execution = runner(gateway).run(config(
				List.of("gpt-5.6-luna"), 1, 1, "1.00"
		));

		assertThat(execution.runs()).singleElement().satisfies(run -> {
			assertThat(run.inputTokens()).isEqualTo(900L);
			assertThat(run.cachedInputTokens()).isEqualTo(100L);
			assertThat(run.cacheWriteTokens()).isEqualTo(25L);
			assertThat(run.outputTokens()).isEqualTo(80L);
			assertThat(run.reasoningTokens()).isEqualTo(15L);
			assertThat(run.totalTokens()).isEqualTo(980L);
			assertThat(run.providerLatencyMs()).isEqualTo(44L);
			assertThat(run.estimatedCostUsd()).isNotNull().isPositive();
			assertThat(run.usedFallback()).isTrue();
			assertThat(run.success()).isFalse();
			assertThat(run.errorCategory()).isEqualTo(OpenAIBenchmarkErrorCategory.FALLBACK);
		});
	}

	@Test
	void marksAValidCandidateUnsuccessfulWhenItsCostCannotBeEstimated() {
		OpenAIUsageMetrics longContextUsage = new OpenAIUsageMetrics(
				272_001L, 0L, 0L, 10L, 0L, 272_011L
		);
		FakeGateway gateway = FakeGateway.withBehavior((prompt, request) ->
				new OpenAIStyleAnalysisGatewayResult(
						validOutput(),
						longContextUsage,
						request.model(),
						17L
				)
		);

		OpenAIBenchmarkExecution execution = runner(gateway).run(config(
				List.of("gpt-5.6-luna"), 1, 10, "1.00"
		));

		assertThat(execution.runs()).singleElement().satisfies(run -> {
			assertThat(run.cityCode()).isEqualTo(CityCode.BERLIN_AFTERDARK_NOMAD.name());
			assertThat(run.estimatedCostUsd()).isNull();
			assertThat(run.success()).isFalse();
			assertThat(run.errorCategory())
					.isEqualTo(OpenAIBenchmarkErrorCategory.COST_ESTIMATE_UNAVAILABLE);
		});
		assertThat(execution.terminationReason())
				.isEqualTo(OpenAIBenchmarkErrorCategory.COST_ESTIMATE_UNAVAILABLE);
	}

	@Test
	void recordsAnUnavailableModelAndContinuesWithTheNextConfiguredModel() {
		FakeGateway gateway = FakeGateway.withBehavior((prompt, request) -> {
			if (request.model().equals("gpt-5.6-luna")) {
				throw NotFoundException.builder()
						.headers(Headers.builder().build())
						.build();
			}
			return successResult(request);
		});

		OpenAIBenchmarkExecution execution = runner(gateway).run(config(
				List.of("gpt-5.6-luna", "gpt-5.6-terra"), 1, 10, "1.00"
		));

		assertThat(execution.terminationReason()).isEqualTo(OpenAIBenchmarkErrorCategory.NONE);
		assertThat(execution.runs()).hasSize(3);
		assertThat(execution.runs().getFirst().model()).isEqualTo("gpt-5.6-luna");
		assertThat(execution.runs().getFirst().errorCategory())
				.isEqualTo(OpenAIBenchmarkErrorCategory.MODEL_UNAVAILABLE);
		assertThat(execution.runs().getFirst().success()).isFalse();
		assertThat(execution.runs().subList(1, 3))
				.allSatisfy(run -> {
					assertThat(run.model()).isEqualTo("gpt-5.6-terra");
					assertThat(run.success()).isTrue();
				});
	}

	@Test
	void authenticationFailureRemainsTheExecutionTerminationReasonWhenUsageIsUnavailable() {
		FakeGateway gateway = FakeGateway.withBehavior((prompt, request) -> {
			throw UnauthorizedException.builder()
					.headers(Headers.builder().build())
					.build();
		});

		OpenAIBenchmarkExecution execution = runner(gateway).run(config(
				List.of("gpt-5.6-luna"), 1, 10, "1.00"
		));

		assertThat(execution.runs()).singleElement().satisfies(run -> {
			assertThat(run.errorCategory()).isEqualTo(OpenAIBenchmarkErrorCategory.AUTHENTICATION);
			assertThat(run.estimatedCostUsd()).isNull();
			assertThat(run.success()).isFalse();
		});
		assertThat(execution.terminationReason())
				.isEqualTo(OpenAIBenchmarkErrorCategory.AUTHENTICATION);
	}

	@Test
	void recordsFallbackAsOpenAIFailureAndDoesNotLogJourneyText() {
		FakeGateway gateway = FakeGateway.withBehavior((prompt, request) -> {
			throw new IllegalStateException("offline provider failure");
		});
		RunnerHarness harness = runnerHarness(gateway);

		OpenAIBenchmarkExecution execution = harness.runner().run(config(
				List.of("gpt-5.6-luna"), 1, 10, "1.00"
		));

		assertThat(execution.runs()).singleElement().satisfies(run -> {
			assertThat(run.usedFallback()).isTrue();
			assertThat(run.success()).isFalse();
			assertThat(run.errorCategory()).isEqualTo(OpenAIBenchmarkErrorCategory.FALLBACK);
			assertThat(run.cityCode()).isEqualTo(CityCode.BERLIN_AFTERDARK_NOMAD.name());
			assertThat(run.estimatedCostUsd()).isNull();
		});
		assertThat(execution.terminationReason())
				.isEqualTo(OpenAIBenchmarkErrorCategory.COST_ESTIMATE_UNAVAILABLE);
		assertThat(harness.output())
				.contains("usedFallback=true", "errorCategory=FALLBACK")
				.doesNotContain("Afterdark movement", "Dynamic movement", "sk-");
	}

	private OpenAIBenchmarkRunner runner(FakeGateway gateway) {
		return runnerHarness(gateway).runner();
	}

	private RunnerHarness runnerHarness(FakeGateway gateway) {
		OpenAIStyleAnalysisProvider provider = new OpenAIStyleAnalysisProvider(
				gateway,
				new StyleAnalysisPromptFactory(),
				"offline-configured-model",
				"none"
		);
		AtomicLong nanos = new AtomicLong();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OpenAIBenchmarkRunner runner = new OpenAIBenchmarkRunner(
				provider,
				new StyleAnalysisValidator(),
				new RuleBasedStyleFallback(),
				new OpenAIEstimatedCostCalculator(),
				OpenAIBenchmarkPricingSnapshot.standardShortContext(),
				new OpenAIBenchmarkErrorClassifier(),
				Clock.fixed(Instant.parse("2026-08-16T00:00:00Z"), ZoneOffset.UTC),
				() -> nanos.getAndAdd(5_000_000L),
				new PrintStream(output, true, StandardCharsets.UTF_8)
		);
		return new RunnerHarness(runner, output);
	}

	private OpenAIBenchmarkConfig config(
			List<String> models,
			int repetitions,
			int maxCalls,
			String costCap
	) {
		return new OpenAIBenchmarkConfig(
				true,
				true,
				models,
				repetitions,
				maxCalls,
				new BigDecimal(costCap),
				OpenAIReasoningEffort.NONE,
				Path.of("build/reports/openai-benchmark-test")
		);
	}

	private static OpenAIStyleAnalysisGatewayResult successResult(
			OpenAIStyleAnalysisGatewayRequest request
	) {
		return new OpenAIStyleAnalysisGatewayResult(
				validOutput(),
				COMPLETE_USAGE,
				request.model(),
				17L
		);
	}

	private static OpenAIStyleAnalysisOutput validOutput() {
		return new OpenAIStyleAnalysisOutput(
				CityCode.BERLIN_AFTERDARK_NOMAD.name(),
				RecommendedProduct.STARK_BACKPACK.name(),
				StyleMood.AFTERDARK_MOVEMENT.name(),
				CityBackground.BERLIN_AFTERDARK.name(),
				"Offline benchmark result",
				91
		);
	}

	private record RunnerHarness(OpenAIBenchmarkRunner runner, ByteArrayOutputStream bytes) {

		String output() {
			return bytes.toString(StandardCharsets.UTF_8);
		}
	}

	@FunctionalInterface
	private interface GatewayBehavior {
		OpenAIStyleAnalysisGatewayResult apply(
				StyleAnalysisPrompt prompt,
				OpenAIStyleAnalysisGatewayRequest request
		);
	}

	private static final class FakeGateway implements OpenAIStyleAnalysisGateway {

		private final GatewayBehavior behavior;
		private final List<OpenAIStyleAnalysisGatewayRequest> requests = new ArrayList<>();

		private FakeGateway(GatewayBehavior behavior) {
			this.behavior = behavior;
		}

		static FakeGateway succeeding() {
			return withBehavior((prompt, request) -> successResult(request));
		}

		static FakeGateway withBehavior(GatewayBehavior behavior) {
			return new FakeGateway(behavior);
		}

		@Override
		public OpenAIStyleAnalysisGatewayResult analyze(
				StyleAnalysisPrompt prompt,
				OpenAIStyleAnalysisGatewayRequest request
		) {
			requests.add(request);
			return behavior.apply(prompt, request);
		}

		List<OpenAIStyleAnalysisGatewayRequest> requests() {
			return List.copyOf(requests);
		}
	}
}
