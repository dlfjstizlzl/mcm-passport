package com.mcm.passport.domain.style.analysis;

import com.mcm.passport.domain.style.entity.CityBackground;
import com.mcm.passport.domain.style.entity.CityCode;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import com.mcm.passport.domain.style.entity.StyleMood;
import org.springframework.stereotype.Component;

@Component
public class StyleAnalysisValidator {

	private static final int MAX_DESCRIPTION_LENGTH = 1_000;

	public ValidatedStyleAnalysis validate(StyleAnalysisCandidate candidate) {
		if (candidate == null) {
			throw new IllegalArgumentException("analysis candidate must not be null");
		}

		CityCode cityCode = parse(CityCode.class, candidate.cityCode());
		RecommendedProduct product = parse(RecommendedProduct.class, candidate.recommendedProduct());
		StyleMood mood = parse(StyleMood.class, candidate.styleMood());
		CityBackground background = parse(CityBackground.class, candidate.background());
		String description = validateDescription(candidate.description());
		int matchScore = validateMatchScore(candidate.matchScore());

		if (cityCode.getBackground() != background) {
			throw new IllegalArgumentException("background is not curated for the selected city code");
		}

		return new ValidatedStyleAnalysis(
				cityCode,
				product,
				mood,
				background,
				description,
				matchScore
		);
	}

	private <E extends Enum<E>> E parse(Class<E> enumType, String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(enumType.getSimpleName() + " must not be blank");
		}
		return Enum.valueOf(enumType, value);
	}

	private String validateDescription(String description) {
		if (description == null || description.isBlank() || description.length() > MAX_DESCRIPTION_LENGTH) {
			throw new IllegalArgumentException("description is invalid");
		}
		return description;
	}

	private int validateMatchScore(Integer matchScore) {
		if (matchScore == null || matchScore < 0 || matchScore > 100) {
			throw new IllegalArgumentException("match score must be between 0 and 100");
		}
		return matchScore;
	}
}
