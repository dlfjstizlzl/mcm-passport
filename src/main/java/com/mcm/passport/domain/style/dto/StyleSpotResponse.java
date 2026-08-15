package com.mcm.passport.domain.style.dto;

import com.mcm.passport.domain.style.entity.StyleSpot;
import com.mcm.passport.domain.style.entity.StyleSpotStatus;

public record StyleSpotResponse(
		String spotCode,
		StyleSpotStatus status,
		Long passportSessionId
) {

	public static StyleSpotResponse from(StyleSpot styleSpot) {
		Long sessionId = styleSpot.getPassportSession() == null
				? null
				: styleSpot.getPassportSession().getId();
		return new StyleSpotResponse(styleSpot.getCode(), styleSpot.getStatus(), sessionId);
	}
}
