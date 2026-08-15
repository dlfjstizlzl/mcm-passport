package com.mcm.passport.domain.style.dto;

import com.mcm.passport.domain.style.entity.StyleResult;

import java.time.Instant;

public record StyleResultResponse(
		Long id,
		Long passportSessionId,
		String cityCode,
		String cityCodeName,
		String recommendedProductCode,
		String recommendedProductName,
		String styleMood,
		String styleMoodName,
		String backgroundCode,
		String backgroundAssetKey,
		String description,
		int matchScore,
		boolean usedFallback,
		Instant createdAt
) {

	public static StyleResultResponse from(StyleResult styleResult) {
		return new StyleResultResponse(
				styleResult.getId(),
				styleResult.getPassportSession().getId(),
				styleResult.getCityCode().name(),
				styleResult.getCityCode().getDisplayName(),
				styleResult.getRecommendedProduct().name(),
				styleResult.getRecommendedProduct().getDisplayName(),
				styleResult.getStyleMood().name(),
				styleResult.getStyleMood().getDisplayName(),
				styleResult.getBackground().name(),
				styleResult.getBackground().getAssetKey(),
				styleResult.getDescription(),
				styleResult.getMatchScore(),
				styleResult.isUsedFallback(),
				styleResult.getCreatedAt()
		);
	}
}
