package com.mcm.passport.domain.style.dto;

import com.mcm.passport.domain.style.entity.JourneySouvenir;

import java.time.Instant;
import java.util.List;

public record JourneySouvenirResponse(
		Long id,
		Long passportSessionId,
		Long styleResultId,
		String cityCode,
		String cityCodeName,
		String recommendedProductCode,
		String recommendedProductName,
		String styleMood,
		String styleMoodName,
		String backgroundCode,
		String backgroundAssetKey,
		List<String> journeyStamps,
		List<String> taggedProductCodes,
		Instant createdAt
) {

	public static JourneySouvenirResponse from(JourneySouvenir souvenir) {
		return new JourneySouvenirResponse(
				souvenir.getId(),
				souvenir.getPassportSession().getId(),
				souvenir.getStyleResult().getId(),
				souvenir.getCityCode().name(),
				souvenir.getCityCode().getDisplayName(),
				souvenir.getRecommendedProduct().name(),
				souvenir.getRecommendedProduct().getDisplayName(),
				souvenir.getStyleMood().name(),
				souvenir.getStyleMood().getDisplayName(),
				souvenir.getBackground().name(),
				souvenir.getBackground().getAssetKey(),
				souvenir.getJourneyStamps(),
				souvenir.getTaggedProductCodes(),
				souvenir.getCreatedAt()
		);
	}
}
