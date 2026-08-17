package com.mcm.passport.domain.style.analysis;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;

public interface StyleAnalysisProvider {

	StyleAnalysisCandidate analyze(JourneyDataSnapshot journeyData);
}
