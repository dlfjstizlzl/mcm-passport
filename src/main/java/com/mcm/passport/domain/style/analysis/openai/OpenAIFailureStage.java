package com.mcm.passport.domain.style.analysis.openai;

/** Stable stage names for safe OpenAI failure diagnostics. */
public enum OpenAIFailureStage {
	HTTP_REQUEST,
	SDK_RESPONSE_DESERIALIZATION,
	USAGE_MAPPING,
	RESPONSE_METADATA,
	STRUCTURED_OUTPUT_DESERIALIZATION,
	VALIDATION
}
