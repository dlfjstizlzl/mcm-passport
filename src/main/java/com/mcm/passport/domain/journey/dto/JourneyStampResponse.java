package com.mcm.passport.domain.journey.dto;

import com.mcm.passport.domain.journey.entity.JourneyStamp;

import java.time.Instant;

public record JourneyStampResponse(
		Long journeyStampId,
		Long journeySpotId,
		Instant stampedAt
) {

	public static JourneyStampResponse from(JourneyStamp journeyStamp) {
		return new JourneyStampResponse(
				journeyStamp.getId(),
				journeyStamp.getJourneySpot().getId(),
				journeyStamp.getStampedAt()
		);
	}
}
