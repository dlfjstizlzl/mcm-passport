package com.mcm.passport.domain.style.analysis;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StyleAnalysisEngine {

	private static final Logger log = LoggerFactory.getLogger(StyleAnalysisEngine.class);

	private final StyleAnalysisProvider provider;
	private final StyleAnalysisValidator validator;
	private final RuleBasedStyleFallback fallback;

	public StyleAnalysisEngine(
			StyleAnalysisProvider provider,
			StyleAnalysisValidator validator,
			RuleBasedStyleFallback fallback
	) {
		this.provider = provider;
		this.validator = validator;
		this.fallback = fallback;
	}

	public StyleAnalysisDecision analyze(JourneyDataSnapshot journeyData) {
		try {
			return new StyleAnalysisDecision(
					validator.validate(provider.analyze(journeyData)),
					false
			);
		} catch (RuntimeException exception) {
			log.warn("Style analysis provider returned an unusable result; applying rule-based fallback", exception);
			return new StyleAnalysisDecision(
					validator.validate(fallback.analyze(journeyData)),
					true
			);
		}
	}
}
