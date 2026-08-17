package com.mcm.passport.domain.style.analysis;

public record StyleAnalysisCandidate(
		String cityCode,
		String recommendedProduct,
		String styleMood,
		String background,
		String description,
		Integer matchScore
) {
}
