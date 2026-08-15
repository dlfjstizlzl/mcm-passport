package com.mcm.passport.domain.style.analysis;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.style.entity.CityCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "mcm.style.analysis.provider", havingValue = "mock", matchIfMissing = true)
public class MockStyleAnalysisProvider implements StyleAnalysisProvider {

	@Override
	public StyleAnalysisCandidate analyze(JourneyDataSnapshot journeyData) {
		String signals = collectSignals(journeyData);
		CityCode profile = selectProfile(signals);

		return new StyleAnalysisCandidate(
				profile.name(),
				profile.getDefaultProduct().name(),
				profile.getDefaultMood().name(),
				profile.getBackground().name(),
				profile.getDefaultDescription(),
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
			return CityCode.PARIS_MODERN_VOYAGER;
		}
		if (signals.contains("CREATIVE") || signals.contains("TREND") || signals.contains("AREN")) {
			return CityCode.SEOUL_CREATIVE_MOVER;
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
