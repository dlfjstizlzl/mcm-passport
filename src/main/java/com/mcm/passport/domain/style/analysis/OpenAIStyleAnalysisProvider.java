package com.mcm.passport.domain.style.analysis;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.style.analysis.metrics.OpenAIStyleAnalysisMeasurement;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisGateway;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisGatewayRequest;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisGatewayResult;
import com.mcm.passport.domain.style.analysis.openai.OpenAIReasoningEffort;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisOutput;
import com.mcm.passport.domain.style.analysis.openai.StyleAnalysisPrompt;
import com.mcm.passport.domain.style.analysis.openai.StyleAnalysisPromptFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Responses API provider enabled only when {@code mcm.style.analysis.provider=openai}. */
@Component("openAIStyleAnalysisProvider")
@ConditionalOnProperty(name = "mcm.style.analysis.provider", havingValue = "openai")
public class OpenAIStyleAnalysisProvider implements StyleAnalysisProvider {

	private final OpenAIStyleAnalysisGateway gateway;
	private final StyleAnalysisPromptFactory promptFactory;
	private final OpenAIStyleAnalysisGatewayRequest configuredRequest;

	@Autowired
	public OpenAIStyleAnalysisProvider(
			OpenAIStyleAnalysisGateway gateway,
			StyleAnalysisPromptFactory promptFactory,
			@Value("${mcm.style.analysis.openai.model:}") String model,
			@Value("${mcm.style.analysis.openai.reasoning-effort:none}") String reasoningEffort
	) {
		this(
				gateway,
				promptFactory,
				new OpenAIStyleAnalysisGatewayRequest(
						model,
						OpenAIReasoningEffort.parse(reasoningEffort)
				)
		);
	}

	OpenAIStyleAnalysisProvider(
			OpenAIStyleAnalysisGateway gateway,
			StyleAnalysisPromptFactory promptFactory
	) {
		this(
				gateway,
				promptFactory,
				new OpenAIStyleAnalysisGatewayRequest("offline-test-model", OpenAIReasoningEffort.NONE)
		);
	}

	private OpenAIStyleAnalysisProvider(
			OpenAIStyleAnalysisGateway gateway,
			StyleAnalysisPromptFactory promptFactory,
			OpenAIStyleAnalysisGatewayRequest configuredRequest
	) {
		this.gateway = Objects.requireNonNull(gateway, "OpenAI gateway must not be null");
		this.promptFactory = Objects.requireNonNull(
				promptFactory,
				"Style analysis prompt factory must not be null"
		);
		this.configuredRequest = Objects.requireNonNull(
				configuredRequest,
				"Configured OpenAI request must not be null"
		);
	}

	@Override
	public StyleAnalysisCandidate analyze(JourneyDataSnapshot journeyData) {
		return analyzeWithMetrics(journeyData, configuredRequest).candidate();
	}

	public OpenAIStyleAnalysisMeasurement analyzeWithMetrics(
			JourneyDataSnapshot journeyData,
			OpenAIStyleAnalysisGatewayRequest request
	) {
		Objects.requireNonNull(request, "OpenAI gateway request must not be null");
		StyleAnalysisPrompt prompt = promptFactory.create(journeyData);
		OpenAIStyleAnalysisGatewayResult result = Objects.requireNonNull(
				gateway.analyze(prompt, request),
				"OpenAI gateway result must not be null"
		);
		OpenAIStyleAnalysisOutput output = result.output();

		StyleAnalysisCandidate candidate = new StyleAnalysisCandidate(
				output.cityCode(),
				output.recommendedProduct(),
				output.styleMood(),
				output.background(),
				output.description(),
				output.matchScore()
		);

		return new OpenAIStyleAnalysisMeasurement(
				candidate,
				result.usage(),
				result.model(),
				result.providerLatencyMs()
		);
	}
}
