package com.mcm.passport.domain.journey.dto;

import com.mcm.passport.domain.journey.entity.JourneySpot;
import java.time.Instant;

public record JourneyProgressSpotResponse(
		Long journeySpotId,
		String code,
		String name,
		String description,
		int sequence,
		boolean required,
		boolean completed,
		Instant stampedAt
) {
	public static JourneyProgressSpotResponse from(JourneySpot spot, Instant stampedAt) {
		return new JourneyProgressSpotResponse(spot.getId(), spot.getCode(), spot.getName(), spot.getDescription(),
				spot.getSequence(), spot.isRequired(), stampedAt != null, stampedAt);
	}
}
