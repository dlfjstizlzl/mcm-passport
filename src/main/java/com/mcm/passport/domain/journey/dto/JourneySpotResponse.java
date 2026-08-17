package com.mcm.passport.domain.journey.dto;

import com.mcm.passport.domain.journey.entity.JourneySpot;

public record JourneySpotResponse(
		Long id,
		String code,
		String name,
		String description,
		int sequence,
		boolean required
) {

	public static JourneySpotResponse from(JourneySpot journeySpot) {
		return new JourneySpotResponse(
				journeySpot.getId(),
				journeySpot.getCode(),
				journeySpot.getName(),
				journeySpot.getDescription(),
				journeySpot.getSequence(),
				journeySpot.isRequired()
		);
	}
}
