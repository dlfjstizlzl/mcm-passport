package com.mcm.passport.domain.style.service;

import com.mcm.passport.domain.style.dto.StyleResultResponse;

public record StyleAnalysisPreparation(
		String spotCode,
		Long passportSessionId,
		long analysisAttempt,
		StyleResultResponse existingResult
) {

	public boolean alreadyCompleted() {
		return existingResult != null;
	}
}
