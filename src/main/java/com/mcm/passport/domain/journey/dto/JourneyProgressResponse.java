package com.mcm.passport.domain.journey.dto;

import com.mcm.passport.domain.passport.entity.PassportSession;
import java.util.List;

public record JourneyProgressResponse(
		Long passportSessionId,
		String status,
		List<JourneyProgressSpotResponse> spots
) {
	public static JourneyProgressResponse of(PassportSession session, List<JourneyProgressSpotResponse> spots) {
		return new JourneyProgressResponse(session.getId(), session.getStatus().name(), spots);
	}
}
