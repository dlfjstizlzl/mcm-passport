package com.mcm.passport.domain.style.analysis.openai;

public class OpenAIStructuredOutputException extends RuntimeException {

	public OpenAIStructuredOutputException() {
		super("OpenAI response did not contain typed style analysis output");
	}
}
