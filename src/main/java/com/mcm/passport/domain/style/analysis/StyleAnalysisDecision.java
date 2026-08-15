package com.mcm.passport.domain.style.analysis;

public record StyleAnalysisDecision(
		ValidatedStyleAnalysis analysis,
		boolean usedFallback
) {
}
