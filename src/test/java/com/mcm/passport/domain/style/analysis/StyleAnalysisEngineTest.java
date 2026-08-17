package com.mcm.passport.domain.style.analysis;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.style.entity.CityBackground;
import com.mcm.passport.domain.style.entity.CityCode;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import com.mcm.passport.domain.style.entity.StyleMood;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StyleAnalysisEngineTest {

	private final StyleAnalysisValidator validator = new StyleAnalysisValidator();
	private final RuleBasedStyleFallback fallback = new RuleBasedStyleFallback();

	@Test
	void validProviderResultUsesCuratedValuesWithoutFallback() {
		StyleAnalysisProvider provider = journeyData -> new StyleAnalysisCandidate(
				CityCode.TOKYO_QUIET_MINIMALIST.name(),
				RecommendedProduct.DIAMANT_3D_SHOULDER_BAG.name(),
				StyleMood.MODERN_ELEGANCE.name(),
				CityBackground.TOKYO_QUIET_LINE.name(),
				"A curated modern Paris result.",
				94
		);

		StyleAnalysisDecision decision = engine(provider).analyze(journeyData());

		assertThat(decision.usedFallback()).isFalse();
		assertThat(decision.analysis().cityCode()).isEqualTo(CityCode.TOKYO_QUIET_MINIMALIST);
		assertThat(decision.analysis().recommendedProduct())
				.isEqualTo(RecommendedProduct.DIAMANT_3D_SHOULDER_BAG);
		assertThat(decision.analysis().styleMood()).isEqualTo(StyleMood.MODERN_ELEGANCE);
		assertThat(decision.analysis().background()).isEqualTo(CityBackground.TOKYO_QUIET_LINE);
		assertThat(decision.analysis().matchScore()).isEqualTo(94);
	}

	@Test
	void invalidCityCodeUsesCurrentPrototypeFallback() {
		StyleAnalysisProvider provider = journeyData -> candidate("UNLISTED_CITY", CityBackground.BERLIN_AFTER_DARK.name());

		StyleAnalysisDecision decision = engine(provider).analyze(journeyData());

		assertBerlinFallback(decision);
	}

	@Test
	void invalidProductUsesCurrentPrototypeFallback() {
		StyleAnalysisProvider provider = journeyData -> new StyleAnalysisCandidate(
				CityCode.BERLIN_AFTERDARK_NOMAD.name(),
				"UNLISTED_PRODUCT",
				StyleMood.AFTERDARK_MOVEMENT.name(),
				CityBackground.BERLIN_AFTER_DARK.name(),
				"A provider result that must be validated.",
				92
		);

		StyleAnalysisDecision decision = engine(provider).analyze(journeyData());

		assertBerlinFallback(decision);
	}

	@Test
	void backgroundNotCuratedForCityUsesCurrentPrototypeFallback() {
		StyleAnalysisProvider provider = journeyData -> candidate(
				CityCode.BERLIN_AFTERDARK_NOMAD.name(),
				CityBackground.SEOUL_PULSE.name()
		);

		StyleAnalysisDecision decision = engine(provider).analyze(journeyData());

		assertBerlinFallback(decision);
	}

	@Test
	void providerExceptionUsesCurrentPrototypeFallback() {
		StyleAnalysisProvider provider = journeyData -> {
			throw new IllegalStateException("provider unavailable");
		};

		StyleAnalysisDecision decision = engine(provider).analyze(journeyData());

		assertBerlinFallback(decision);
	}

	@Test
	void currentPrototypeFallbackMayUseOptionalTaggedProductAsAnAdditionalSignal() {
		StyleAnalysisProvider provider = journeyData -> {
			throw new IllegalStateException("provider unavailable");
		};
		JourneyDataSnapshot parisJourney = new JourneyDataSnapshot(
				2L,
				List.of(new JourneyDataSnapshot.ResponseSignal(
						"MATERIAL_LOUNGE",
						"TODAY_MOOD",
						"BALANCED",
						"Balanced movement"
				)),
				List.of(new JourneyDataSnapshot.StampSignal("MATERIAL_LOUNGE")),
				List.of(new JourneyDataSnapshot.ProductSignal(
						20L,
						RecommendedProduct.DIAMANT_3D_SHOULDER_BAG.name(),
						RecommendedProduct.DIAMANT_3D_SHOULDER_BAG.getDisplayName()
				))
		);

		StyleAnalysisDecision decision = engine(provider).analyze(parisJourney);

		assertThat(decision.usedFallback()).isTrue();
		assertThat(decision.analysis().cityCode()).isEqualTo(CityCode.TOKYO_QUIET_MINIMALIST);
		assertThat(decision.analysis().recommendedProduct())
				.isEqualTo(RecommendedProduct.DIAMANT_3D_SHOULDER_BAG);
		assertThat(decision.analysis().background()).isEqualTo(CityBackground.TOKYO_QUIET_LINE);
	}

	private StyleAnalysisEngine engine(StyleAnalysisProvider provider) {
		return new StyleAnalysisEngine(provider, validator, fallback);
	}

	private StyleAnalysisCandidate candidate(String cityCode, String background) {
		return new StyleAnalysisCandidate(
				cityCode,
				RecommendedProduct.STARK_BACKPACK.name(),
				StyleMood.AFTERDARK_MOVEMENT.name(),
				background,
				"A provider result that must be validated.",
				92
		);
	}

	private JourneyDataSnapshot journeyData() {
		return new JourneyDataSnapshot(
				1L,
				List.of(new JourneyDataSnapshot.ResponseSignal(
						"CITY_MOOD_ROOM",
						"TODAY_MOOD",
						"AFTERDARK",
						"Afterdark movement"
				)),
				List.of(new JourneyDataSnapshot.StampSignal("CITY_MOOD_ROOM")),
				List.of(new JourneyDataSnapshot.ProductSignal(
						10L,
						RecommendedProduct.STARK_BACKPACK.name(),
						RecommendedProduct.STARK_BACKPACK.getDisplayName()
				))
		);
	}

	private void assertBerlinFallback(StyleAnalysisDecision decision) {
		assertThat(decision.usedFallback()).isTrue();
		assertThat(decision.analysis().cityCode()).isEqualTo(CityCode.BERLIN_AFTERDARK_NOMAD);
		assertThat(decision.analysis().recommendedProduct()).isEqualTo(RecommendedProduct.STARK_BACKPACK);
		assertThat(decision.analysis().styleMood()).isEqualTo(StyleMood.AFTERDARK_MOVEMENT);
		assertThat(decision.analysis().background()).isEqualTo(CityBackground.BERLIN_AFTER_DARK);
		assertThat(decision.analysis().matchScore()).isEqualTo(85);
	}
}
