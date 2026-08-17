package com.mcm.passport.domain.style.dto;

import com.mcm.passport.domain.style.entity.StyleSpotStatus;

public record StyleSpotDisplayResponse(
		String styleSpotId,
		Long activeStyleSpotSessionId,
		StyleSpotStatus status,
		StyleResultResponse styleResult
) {
}
