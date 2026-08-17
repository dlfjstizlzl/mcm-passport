package com.mcm.passport.domain.style.analysis.openai;

import java.util.Objects;

public record StyleAnalysisPrompt(String instructions, String input) {

	public StyleAnalysisPrompt {
		if (Objects.requireNonNull(instructions, "instructions must not be null").isBlank()) {
			throw new IllegalArgumentException("instructions must not be blank");
		}
		if (Objects.requireNonNull(input, "input must not be null").isBlank()) {
			throw new IllegalArgumentException("input must not be blank");
		}
	}
}
