package com.mcm.passport.domain.style.analysis;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.style.analysis.metrics.OpenAIStyleAnalysisMeasurement;
import com.mcm.passport.domain.style.analysis.metrics.OpenAIUsageMetrics;
import com.mcm.passport.domain.style.analysis.openai.OpenAIReasoningEffort;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisGateway;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisGatewayRequest;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisGatewayResult;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisOutput;
import com.mcm.passport.domain.style.analysis.openai.StyleAnalysisPrompt;
import com.mcm.passport.domain.style.analysis.openai.StyleAnalysisPromptFactory;
import com.mcm.passport.domain.style.analysis.openai.StyleAnalysisPromptInput;
import com.mcm.passport.domain.style.entity.CityBackground;
import com.mcm.passport.domain.style.entity.CityCode;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import com.mcm.passport.domain.style.entity.StyleMood;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAIStyleAnalysisProviderTest {

	private final StyleAnalysisPromptFactory promptFactory = new StyleAnalysisPromptFactory();
	private final StyleAnalysisValidator validator = new StyleAnalysisValidator();
	private final RuleBasedStyleFallback fallback = new RuleBasedStyleFallback();

	@Test
	void officialSdkBuildsTheStructuredOutputSchemaOffline() {
		StructuredResponseCreateParams<OpenAIStyleAnalysisOutput> params = ResponseCreateParams.builder()
				.input("offline schema contract")
				.text(OpenAIStyleAnalysisOutput.class)
				.model("not-sent")
				.build();

		assertThat(params.rawParams()).isNotNull();
	}

	@Test
	void mapsStructuredGatewayOutputToStyleAnalysisCandidateOffline() {
		FakeGateway gateway = FakeGateway.returning(validBerlinOutput());
		OpenAIStyleAnalysisProvider provider = provider(gateway);

		StyleAnalysisCandidate candidate = provider.analyze(journeyWithoutProductTags());

		assertThat(candidate.cityCode()).isEqualTo(CityCode.BERLIN_AFTERDARK_NOMAD.name());
		assertThat(candidate.recommendedProduct()).isEqualTo(RecommendedProduct.STARK_BACKPACK.name());
		assertThat(candidate.styleMood()).isEqualTo(StyleMood.AFTERDARK_MOVEMENT.name());
		assertThat(candidate.background()).isEqualTo(CityBackground.BERLIN_AFTER_DARK.name());
		assertThat(candidate.description()).isEqualTo("오늘 Journey를 위한 오프라인 테스트 결과입니다.");
		assertThat(candidate.matchScore()).isEqualTo(91);
		assertThat(gateway.capturedPrompt()).isNotNull();
		assertThat(gateway.capturedRequest().model()).isEqualTo("offline-test-model");
		assertThat(gateway.capturedRequest().effort()).isEqualTo(OpenAIReasoningEffort.NONE);
	}

	@Test
	void preservesOfflineGatewayUsageModelAndLatencyInMeasurement() {
		FakeGateway gateway = FakeGateway.returning(validBerlinOutput());
		OpenAIStyleAnalysisProvider provider = provider(gateway);
		OpenAIStyleAnalysisGatewayRequest request = new OpenAIStyleAnalysisGatewayRequest(
				"gpt-5.6-luna",
				OpenAIReasoningEffort.LOW
		);

		OpenAIStyleAnalysisMeasurement measurement = provider.analyzeWithMetrics(
				journeyWithoutProductTags(),
				request
		);

		assertThat(measurement.candidate().cityCode()).isEqualTo(CityCode.BERLIN_AFTERDARK_NOMAD.name());
		assertThat(measurement.usage()).isEqualTo(FakeGateway.USAGE);
		assertThat(measurement.model()).isEqualTo("gpt-5.6-luna");
		assertThat(measurement.providerLatencyMs()).isEqualTo(23L);
		assertThat(gateway.capturedRequest()).isEqualTo(request);
	}

	@Test
	void buildsPromptInputWhenOptionalProductTagIsAbsent() {
		JourneyDataSnapshot journeyData = journeyWithoutProductTags();

		StyleAnalysisPromptInput input = StyleAnalysisPromptInput.from(journeyData);
		StyleAnalysisPrompt prompt = promptFactory.create(journeyData);

		assertThat(input.responses()).hasSize(1);
		assertThat(input.stamps()).hasSize(1);
		assertThat(input.taggedProducts()).isEmpty();
		assertThat(input.catalog().cityCodes()).contains(CityCode.BERLIN_AFTERDARK_NOMAD.name());
		assertThat(input.catalog().recommendedProducts()).contains(RecommendedProduct.STARK_BACKPACK.name());
		assertThat(prompt.instructions()).contains("선택적인 ProductTag", "영구적인 성향");
		assertThat(prompt.input())
				.contains("responses:", "stamps:", "taggedProducts (optional):", "prototypeCatalog:")
				.contains("AFTERDARK", "CITY_MOOD_ROOM")
				.doesNotContain("sessionId");
	}

	@Test
	void invalidCityFromOpenAIStructuredOutputUsesExistingValidatorFallback() {
		OpenAIStyleAnalysisOutput invalidCity = new OpenAIStyleAnalysisOutput(
				"UNLISTED_CITY",
				RecommendedProduct.STARK_BACKPACK.name(),
				StyleMood.AFTERDARK_MOVEMENT.name(),
				CityBackground.BERLIN_AFTER_DARK.name(),
				"Validator가 거부해야 하는 테스트 출력입니다.",
				91
		);

		StyleAnalysisDecision decision = engine(FakeGateway.returning(invalidCity))
				.analyze(journeyWithoutProductTags());

		assertBerlinPrototypeFallback(decision);
	}

	@Test
	void incompatibleCityBackgroundFromOpenAIUsesExistingValidatorFallback() {
		OpenAIStyleAnalysisOutput incompatibleBackground = new OpenAIStyleAnalysisOutput(
				CityCode.BERLIN_AFTERDARK_NOMAD.name(),
				RecommendedProduct.STARK_BACKPACK.name(),
				StyleMood.AFTERDARK_MOVEMENT.name(),
				CityBackground.SEOUL_PULSE.name(),
				"Validator가 거부해야 하는 테스트 출력입니다.",
				91
		);

		StyleAnalysisDecision decision = engine(FakeGateway.returning(incompatibleBackground))
				.analyze(journeyWithoutProductTags());

		assertBerlinPrototypeFallback(decision);
	}

	@Test
	void gatewayApiFailureUsesExistingFallbackWithoutNetworkAccess() {
		FakeGateway gateway = FakeGateway.failingWith(
				new IllegalStateException("fake OpenAI API unavailable")
		);

		StyleAnalysisDecision decision = engine(gateway).analyze(journeyWithoutProductTags());

		assertBerlinPrototypeFallback(decision);
		assertThat(gateway.capturedPrompt()).isNotNull();
	}

	@Test
	void structuredOutputParsingFailureUsesExistingFallbackWithoutNetworkAccess() {
		FakeGateway gateway = FakeGateway.failingWith(
				new IllegalArgumentException("fake structured output deserialization failure")
		);

		StyleAnalysisDecision decision = engine(gateway).analyze(journeyWithoutProductTags());

		assertBerlinPrototypeFallback(decision);
		assertThat(gateway.capturedPrompt()).isNotNull();
	}

	private OpenAIStyleAnalysisProvider provider(OpenAIStyleAnalysisGateway gateway) {
		return new OpenAIStyleAnalysisProvider(gateway, promptFactory);
	}

	private StyleAnalysisEngine engine(OpenAIStyleAnalysisGateway gateway) {
		return new StyleAnalysisEngine(provider(gateway), validator, fallback);
	}

	private JourneyDataSnapshot journeyWithoutProductTags() {
		return new JourneyDataSnapshot(
				42L,
				List.of(new JourneyDataSnapshot.ResponseSignal(
						"CITY_MOOD_ROOM",
						"TODAY_MOOD",
						"AFTERDARK",
						"Afterdark movement"
				)),
				List.of(new JourneyDataSnapshot.StampSignal("CITY_MOOD_ROOM")),
				List.of()
		);
	}

	private OpenAIStyleAnalysisOutput validBerlinOutput() {
		return new OpenAIStyleAnalysisOutput(
				CityCode.BERLIN_AFTERDARK_NOMAD.name(),
				RecommendedProduct.STARK_BACKPACK.name(),
				StyleMood.AFTERDARK_MOVEMENT.name(),
				CityBackground.BERLIN_AFTER_DARK.name(),
				"오늘 Journey를 위한 오프라인 테스트 결과입니다.",
				91
		);
	}

	private void assertBerlinPrototypeFallback(StyleAnalysisDecision decision) {
		assertThat(decision.usedFallback()).isTrue();
		assertThat(decision.analysis().cityCode()).isEqualTo(CityCode.BERLIN_AFTERDARK_NOMAD);
		assertThat(decision.analysis().recommendedProduct()).isEqualTo(RecommendedProduct.STARK_BACKPACK);
		assertThat(decision.analysis().background()).isEqualTo(CityBackground.BERLIN_AFTER_DARK);
	}

	private static final class FakeGateway implements OpenAIStyleAnalysisGateway {

		private static final OpenAIUsageMetrics USAGE = new OpenAIUsageMetrics(
				120L,
				20L,
				5L,
				40L,
				10L,
				160L
		);

		private final BiFunction<
				StyleAnalysisPrompt,
				OpenAIStyleAnalysisGatewayRequest,
				OpenAIStyleAnalysisGatewayResult
		> behavior;
		private StyleAnalysisPrompt capturedPrompt;
		private OpenAIStyleAnalysisGatewayRequest capturedRequest;

		private FakeGateway(BiFunction<
				StyleAnalysisPrompt,
				OpenAIStyleAnalysisGatewayRequest,
				OpenAIStyleAnalysisGatewayResult
		> behavior) {
			this.behavior = behavior;
		}

		static FakeGateway returning(OpenAIStyleAnalysisOutput output) {
			return new FakeGateway((prompt, request) -> new OpenAIStyleAnalysisGatewayResult(
					output,
					USAGE,
					request.model(),
					23L
			));
		}

		static FakeGateway failingWith(RuntimeException exception) {
			return new FakeGateway((prompt, request) -> {
				throw exception;
			});
		}

		@Override
		public OpenAIStyleAnalysisGatewayResult analyze(
				StyleAnalysisPrompt prompt,
				OpenAIStyleAnalysisGatewayRequest request
		) {
			capturedPrompt = prompt;
			capturedRequest = request;
			return behavior.apply(prompt, request);
		}

		StyleAnalysisPrompt capturedPrompt() {
			return capturedPrompt;
		}

		OpenAIStyleAnalysisGatewayRequest capturedRequest() {
			return capturedRequest;
		}
	}
}
