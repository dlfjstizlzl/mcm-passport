package com.mcm.passport.domain.journey.dto;

import com.mcm.passport.domain.journey.entity.JourneySpot;
import java.time.Instant;

public record JourneyProgressSpotResponse(
		Long id,
		String code,
		String name,
		int sequence,
		boolean required,
		boolean completed,
		Instant stampedAt
) {
	public static JourneyProgressSpotResponse from(JourneySpot spot, Instant stampedAt) {
		return new JourneyProgressSpotResponse(spot.getId(), spot.getCode(), spot.getName(),
				spot.getSequence(), spot.isRequired(), stampedAt != null, stampedAt);
	}
}
