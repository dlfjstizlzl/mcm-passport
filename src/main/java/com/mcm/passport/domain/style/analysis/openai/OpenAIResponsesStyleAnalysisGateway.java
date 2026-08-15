package com.mcm.passport.domain.style.analysis.openai;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mcm.style.analysis.provider", havingValue = "openai")
public class OpenAIResponsesStyleAnalysisGateway implements OpenAIStyleAnalysisGateway {

	private final OpenAIClient client;
	private final String model;

	public OpenAIResponsesStyleAnalysisGateway(
			OpenAIClient client,
			@Value("${mcm.style.analysis.openai.model:}") String model
	) {
		if (model == null || model.isBlank()) {
			throw new IllegalStateException(
					"OPENAI_MODEL must be configured when mcm.style.analysis.provider=openai"
			);
		}
		this.client = client;
		this.model = model.trim();
	}

	@Override
	public OpenAIStyleAnalysisOutput analyze(StyleAnalysisPrompt prompt) {
		StructuredResponseCreateParams<OpenAIStyleAnalysisOutput> params = ResponseCreateParams.builder()
				.input(prompt.input())
				.instructions(prompt.instructions())
				.text(OpenAIStyleAnalysisOutput.class)
				.model(model)
				.build();

		return client.responses().create(params).output().stream()
				.flatMap(item -> item.message().stream())
				.flatMap(message -> message.content().stream())
				.flatMap(content -> content.outputText().stream())
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("OpenAI response did not contain parsed output text"));
	}
}
