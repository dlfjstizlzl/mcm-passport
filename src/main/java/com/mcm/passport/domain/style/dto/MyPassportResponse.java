package com.mcm.passport.domain.style.dto;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.passport.entity.PassportSessionStatus;

import java.time.Instant;

public record MyPassportResponse(
		Long passportSessionId,
		String cardUid,
		PassportSessionStatus status,
		Instant startedAt,
		Instant completedAt,
		JourneyDataSnapshot journey,
		StyleResultResponse styleResult,
		StylePortraitResponse portrait,
		JourneySouvenirResponse souvenir
) {
}
