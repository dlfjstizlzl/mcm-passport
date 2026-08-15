package com.mcm.passport.domain.demo.benchmark;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import java.util.Objects;

public record OpenAIBenchmarkCase(String name, JourneyDataSnapshot journeyData) {

	public OpenAIBenchmarkCase {
		Objects.requireNonNull(name, "name must not be null");
		if (name.isBlank()) {
			throw new IllegalArgumentException("name must not be blank");
		}
		Objects.requireNonNull(journeyData, "journeyData must not be null");
	}
}
