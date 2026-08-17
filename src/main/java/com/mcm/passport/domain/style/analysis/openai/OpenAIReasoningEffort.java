package com.mcm.passport.domain.style.analysis.openai;

import com.openai.models.ReasoningEffort;

import java.util.Locale;

public enum OpenAIReasoningEffort {

	NONE(ReasoningEffort.NONE),
	LOW(ReasoningEffort.LOW),
	MEDIUM(ReasoningEffort.MEDIUM),
	HIGH(ReasoningEffort.HIGH),
	XHIGH(ReasoningEffort.XHIGH),
	MAX(ReasoningEffort.MAX);

	private final ReasoningEffort sdkValue;

	OpenAIReasoningEffort(ReasoningEffort sdkValue) {
		this.sdkValue = sdkValue;
	}

	public String value() {
		return name().toLowerCase(Locale.ROOT);
	}

	public ReasoningEffort toSdk() {
		return sdkValue;
	}

	public static OpenAIReasoningEffort parse(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("OpenAI reasoning effort must not be blank");
		}

		try {
			return valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Unsupported OpenAI reasoning effort: " + value, exception);
		}
	}
}
