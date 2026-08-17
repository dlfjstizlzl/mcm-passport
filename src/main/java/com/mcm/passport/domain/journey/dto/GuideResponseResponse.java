package com.mcm.passport.domain.journey.dto;

import com.mcm.passport.domain.journey.entity.GuideResponse;

import java.time.Instant;

public record GuideResponseResponse(
		Long guideResponseId,
		Long questionId,
		Long optionId,
		Instant answeredAt
) {

	public static GuideResponseResponse from(GuideResponse guideResponse) {
		return new GuideResponseResponse(
				guideResponse.getId(),
				guideResponse.getGuideQuestion().getId(),
				guideResponse.getGuideOption().getId(),
				guideResponse.getAnsweredAt()
		);
	}
}
