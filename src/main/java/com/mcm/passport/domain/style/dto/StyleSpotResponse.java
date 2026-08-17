package com.mcm.passport.domain.style.dto;

import com.mcm.passport.domain.style.entity.StyleSpot;
import com.mcm.passport.domain.style.entity.StyleSpotSession;
import com.mcm.passport.domain.style.entity.StyleSpotStatus;

public record StyleSpotResponse(
		String spotCode,
		StyleSpotStatus status,
		Long styleSpotSessionId,
		Long passportSessionId
) {

	public static StyleSpotResponse from(StyleSpot styleSpot, StyleSpotSession activeSession) {
		Long connectionId = activeSession == null ? null : activeSession.getId();
		Long passportSessionId = activeSession == null ? null : activeSession.getPassportSession().getId();
		return new StyleSpotResponse(styleSpot.getCode(), styleSpot.getStatus(), connectionId, passportSessionId);
	}
}
