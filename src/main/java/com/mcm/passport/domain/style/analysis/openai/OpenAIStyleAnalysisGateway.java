package com.mcm.passport.domain.style.analysis.openai;

/** Small network boundary so provider behavior can be tested without an OpenAI connection. */
public interface OpenAIStyleAnalysisGateway {

	OpenAIStyleAnalysisGatewayResult analyze(
			StyleAnalysisPrompt prompt,
			OpenAIStyleAnalysisGatewayRequest request
	);
}
