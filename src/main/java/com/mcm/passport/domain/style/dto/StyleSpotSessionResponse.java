package com.mcm.passport.domain.style.dto;

import com.mcm.passport.domain.style.entity.StyleSpotSession;
import com.mcm.passport.domain.style.entity.StyleSpotSessionStatus;

import java.time.Instant;

public record StyleSpotSessionResponse(
		Long id,
		String styleSpotId,
		Long passportSessionId,
		StyleSpotSessionStatus status,
		Instant connectedAt,
		Instant disconnectedAt
) {

	public static StyleSpotSessionResponse from(StyleSpotSession session) {
		return new StyleSpotSessionResponse(
				session.getId(),
				session.getStyleSpot().getCode(),
				session.getPassportSession().getId(),
				session.getStatus(),
				session.getConnectedAt(),
				session.getDisconnectedAt()
		);
	}
}
