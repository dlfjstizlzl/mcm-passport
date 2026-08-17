package com.mcm.passport.domain.style.dto;

import com.mcm.passport.domain.style.entity.StylePortrait;

import java.time.Instant;

public record StylePortraitResponse(
		Long id,
		Long passportSessionId,
		String imageUrl,
		boolean consent,
		Instant createdAt
) {

	public static StylePortraitResponse from(StylePortrait portrait) {
		return new StylePortraitResponse(
				portrait.getId(),
				portrait.getPassportSession().getId(),
				portrait.getImageUrl(),
				portrait.isConsent(),
				portrait.getCreatedAt()
		);
	}
}
