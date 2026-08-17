package com.mcm.passport.domain.journey.service;

import java.util.List;
import java.util.Objects;

public record JourneyDataSnapshot(
		Long sessionId,
		List<ResponseSignal> responses,
		List<StampSignal> stamps,
		List<ProductSignal> taggedProducts
) {

	public JourneyDataSnapshot {
		Objects.requireNonNull(sessionId, "sessionId must not be null");
		responses = List.copyOf(Objects.requireNonNull(responses, "responses must not be null"));
		stamps = List.copyOf(Objects.requireNonNull(stamps, "stamps must not be null"));
		taggedProducts = List.copyOf(Objects.requireNonNull(taggedProducts, "taggedProducts must not be null"));
	}

	public record ResponseSignal(
			String spotCode,
			String questionCode,
			String answerCode,
			String answerText
	) {
	}

	public record StampSignal(String spotCode) {
	}

	public record ProductSignal(Long productId, String code, String name) {
	}
}
