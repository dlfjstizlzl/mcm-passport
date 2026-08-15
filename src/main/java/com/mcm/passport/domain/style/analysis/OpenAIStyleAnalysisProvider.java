package com.mcm.passport.domain.style.analysis;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisGateway;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisOutput;
import com.mcm.passport.domain.style.analysis.openai.StyleAnalysisPrompt;
import com.mcm.passport.domain.style.analysis.openai.StyleAnalysisPromptFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Responses API provider enabled only when {@code mcm.style.analysis.provider=openai}. */
@Component("openAIStyleAnalysisProvider")
@ConditionalOnProperty(name = "mcm.style.analysis.provider", havingValue = "openai")
public class OpenAIStyleAnalysisProvider implements StyleAnalysisProvider {

	private final OpenAIStyleAnalysisGateway gateway;
	private final StyleAnalysisPromptFactory promptFactory;

	public OpenAIStyleAnalysisProvider(
			OpenAIStyleAnalysisGateway gateway,
			StyleAnalysisPromptFactory promptFactory
	) {
		this.gateway = gateway;
		this.promptFactory = promptFactory;
	}

	@Override
	public StyleAnalysisCandidate analyze(JourneyDataSnapshot journeyData) {
		StyleAnalysisPrompt prompt = promptFactory.create(journeyData);
		OpenAIStyleAnalysisOutput output = Objects.requireNonNull(
				gateway.analyze(prompt),
				"OpenAI Structured Output must not be null"
		);

		return new StyleAnalysisCandidate(
				output.cityCode(),
				output.recommendedProduct(),
				output.styleMood(),
				output.background(),
				output.description(),
				output.matchScore()
		);
	}
}
