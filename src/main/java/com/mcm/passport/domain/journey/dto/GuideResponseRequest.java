package com.mcm.passport.domain.journey.dto;

import jakarta.validation.constraints.NotNull;

public record GuideResponseRequest(
		@NotNull(message = "optionId는 필수입니다.")
		Long optionId
) {
}
