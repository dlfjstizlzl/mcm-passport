package com.mcm.passport.domain.journey.dto;

import jakarta.validation.constraints.Size;

public record GuideResponseRequest(
		Long optionId,
		@Size(max = 500, message = "answerText는 500자 이하여야 합니다.")
		String answerText
) {
}
