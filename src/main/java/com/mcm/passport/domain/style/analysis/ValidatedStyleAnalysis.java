package com.mcm.passport.domain.style.analysis;

import com.mcm.passport.domain.style.entity.CityBackground;
import com.mcm.passport.domain.style.entity.CityCode;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import com.mcm.passport.domain.style.entity.StyleMood;

public record ValidatedStyleAnalysis(
		CityCode cityCode,
		RecommendedProduct recommendedProduct,
		StyleMood styleMood,
		CityBackground background,
		String description,
		int matchScore
) {
}
