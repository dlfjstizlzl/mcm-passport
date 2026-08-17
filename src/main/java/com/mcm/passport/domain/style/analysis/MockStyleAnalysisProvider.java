package com.mcm.passport.domain.style.analysis;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.style.catalog.PrototypeStyleCatalog;
import com.mcm.passport.domain.style.entity.CityCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Prototype-only deterministic provider used to exercise the complete backend flow without an
 * external AI call. Replace this bean through {@link StyleAnalysisProvider} for production.
 */
@Component("prototypeMockStyleAnalysisProvider")
@ConditionalOnProperty(name = "mcm.style.analysis.provider", havingValue = "mock", matchIfMissing = true)
public class MockStyleAnalysisProvider implements StyleAnalysisProvider {

	@Override
	public StyleAnalysisCandidate analyze(JourneyDataSnapshot journeyData) {
		String signals = collectSignals(journeyData);
		CityCode cityCode = selectProfile(signals);
		PrototypeStyleCatalog.CityProfile profile = PrototypeStyleCatalog.cityProfile(cityCode);

		return new StyleAnalysisCandidate(
				cityCode.name(),
				profile.defaultProduct().name(),
				profile.defaultMood().name(),
				profile.background().name(),
				profile.defaultDescription(),
				matchScore(journeyData)
		);
	}

	private String collectSignals(JourneyDataSnapshot journeyData) {
		String responses = journeyData.responses().stream()
				.map(response -> response.answerCode() + " " + response.answerText())
				.collect(Collectors.joining(" "));
		String products = journeyData.taggedProducts().stream()
				.map(product -> product.code() + " " + product.name())
				.collect(Collectors.joining(" "));
		return (responses + " " + products).toUpperCase(Locale.ROOT);
	}

	private CityCode selectProfile(String signals) {
		if (signals.contains("ELEGANT") || signals.contains("CLASSIC") || signals.contains("DIAMANT")) {
			return CityCode.TOKYO_QUIET_MINIMALIST;
		}
		if (signals.contains("CREATIVE") || signals.contains("TREND") || signals.contains("AREN")) {
			return CityCode.SEOUL_NEON_PLAYER;
		}
		return CityCode.BERLIN_AFTERDARK_NOMAD;
	}

	private int matchScore(JourneyDataSnapshot journeyData) {
		int signalCount = journeyData.responses().size()
				+ journeyData.stamps().size()
				+ journeyData.taggedProducts().size();
		return Math.min(96, 84 + signalCount);
	}
}
