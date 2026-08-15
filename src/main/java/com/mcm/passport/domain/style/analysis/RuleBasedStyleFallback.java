package com.mcm.passport.domain.style.analysis;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.style.catalog.PrototypeStyleCatalog;
import com.mcm.passport.domain.style.entity.CityCode;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Prototype-only deterministic fallback over the curated MVP catalog. It keeps the flow usable
 * when the configured {@link StyleAnalysisProvider} fails or returns an invalid candidate.
 */
@Component("prototypeRuleBasedStyleFallback")
public class RuleBasedStyleFallback {

	public StyleAnalysisCandidate analyze(JourneyDataSnapshot journeyData) {
		CityCode cityCode = selectProfile(journeyData);
		PrototypeStyleCatalog.CityProfile profile = PrototypeStyleCatalog.cityProfile(cityCode);
		RecommendedProduct product = firstCuratedTaggedProduct(journeyData, profile.defaultProduct());

		return new StyleAnalysisCandidate(
				cityCode.name(),
				product.name(),
				profile.defaultMood().name(),
				profile.background().name(),
				profile.defaultDescription(),
				85
		);
	}

	private CityCode selectProfile(JourneyDataSnapshot journeyData) {
		String taggedProducts = journeyData.taggedProducts().stream()
				.map(product -> product.code() + " " + product.name())
				.collect(Collectors.joining(" "));
		String responses = journeyData.responses().stream()
				.map(response -> response.answerCode() + " " + response.answerText())
				.collect(Collectors.joining(" "));
		String signals = (taggedProducts + " " + responses).toUpperCase(Locale.ROOT);

		if (signals.contains("DIAMANT") || signals.contains("ELEGANT") || signals.contains("CLASSIC")) {
			return CityCode.PARIS_MODERN_VOYAGER;
		}
		if (signals.contains("AREN") || signals.contains("CREATIVE") || signals.contains("TREND")) {
			return CityCode.SEOUL_CREATIVE_MOVER;
		}
		if (signals.contains("STARK") || signals.contains("AFTERDARK") || signals.contains("DYNAMIC")) {
			return CityCode.BERLIN_AFTERDARK_NOMAD;
		}
		return CityCode.SEOUL_CREATIVE_MOVER;
	}

	private RecommendedProduct firstCuratedTaggedProduct(
			JourneyDataSnapshot journeyData,
			RecommendedProduct defaultProduct
	) {
		return journeyData.taggedProducts().stream()
				.map(JourneyDataSnapshot.ProductSignal::code)
				.map(this::toCuratedProduct)
				.filter(product -> product != null)
				.findFirst()
				.orElse(defaultProduct);
	}

	private RecommendedProduct toCuratedProduct(String code) {
		try {
			return RecommendedProduct.valueOf(code);
		} catch (IllegalArgumentException | NullPointerException exception) {
			return null;
		}
	}
}
