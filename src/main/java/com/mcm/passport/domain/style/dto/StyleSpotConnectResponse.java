package com.mcm.passport.domain.style.dto;

import com.mcm.passport.domain.style.entity.StyleSpotStatus;

public record StyleSpotConnectResponse(
		Long styleSpotSessionId,
		String styleSpotId,
		Long passportSessionId,
		StyleSpotStatus status,
		StyleResultResponse styleResult
) {
}
