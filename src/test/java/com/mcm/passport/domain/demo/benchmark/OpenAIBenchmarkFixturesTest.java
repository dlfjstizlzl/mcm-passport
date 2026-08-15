package com.mcm.passport.domain.demo.benchmark;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAIBenchmarkFixturesTest {

	private static final List<String> EXPECTED_STAMPS = List.of(
			"ORIGIN_GATE",
			"MATERIAL_LOUNGE",
			"MOVEMENT_DECK",
			"CITY_MOOD_ROOM"
	);

	@Test
	void caseAContainsTheExactJourneySignalsWithoutAnOptionalProductTag() {
		OpenAIBenchmarkCase caseA = findCase("CASE_A_WITHOUT_PRODUCT_TAG");

		assertCommonJourneySignals(caseA.journeyData());
		assertThat(caseA.journeyData().taggedProducts()).isEmpty();
	}

	@Test
	void caseBOnlyAddsStarkBackpackToTheSameImmutableJourneySignals() {
		OpenAIBenchmarkCase caseA = findCase("CASE_A_WITHOUT_PRODUCT_TAG");
		OpenAIBenchmarkCase caseB = findCase("CASE_B_WITH_PRODUCT_TAG");

		assertCommonJourneySignals(caseB.journeyData());
		assertThat(caseB.journeyData().responses()).isEqualTo(caseA.journeyData().responses());
		assertThat(caseB.journeyData().stamps()).isEqualTo(caseA.journeyData().stamps());
		assertThat(caseB.journeyData().taggedProducts())
				.singleElement()
				.satisfies(product -> {
					assertThat(product.code()).isEqualTo(RecommendedProduct.STARK_BACKPACK.name());
					assertThat(product.name()).isEqualTo(RecommendedProduct.STARK_BACKPACK.getDisplayName());
				});
		assertThatThrownBy(() -> OpenAIBenchmarkFixtures.cases().clear())
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void fixedPromptsStayWithinTheConservativeInputReservationContract() {
		assertThat(OpenAIBenchmarkFixtures.cases())
				.allSatisfy(benchmarkCase -> assertThat(
						OpenAIBenchmarkRunner.renderedPromptUtf8Bytes(benchmarkCase)
				).isLessThanOrEqualTo(OpenAIBenchmarkRunner.MAX_RENDERED_PROMPT_UTF8_BYTES));
	}

	private OpenAIBenchmarkCase findCase(String name) {
		assertThat(OpenAIBenchmarkFixtures.cases()).hasSize(2);
		return OpenAIBenchmarkFixtures.cases().stream()
				.filter(benchmarkCase -> benchmarkCase.name().equals(name))
				.findFirst()
				.orElseThrow();
	}

	private void assertCommonJourneySignals(JourneyDataSnapshot journeyData) {
		assertThat(journeyData.responses())
				.extracting(
						JourneyDataSnapshot.ResponseSignal::spotCode,
						JourneyDataSnapshot.ResponseSignal::questionCode,
						JourneyDataSnapshot.ResponseSignal::answerCode,
						JourneyDataSnapshot.ResponseSignal::answerText
				)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(
								"CITY_MOOD_ROOM",
								"TODAY_MOOD",
								"AFTERDARK",
								"Afterdark movement"
						),
						org.assertj.core.groups.Tuple.tuple(
								"MOVEMENT_DECK",
								"PACE",
								"DYNAMIC",
								"Dynamic movement"
						)
				);
		assertThat(journeyData.stamps())
				.extracting(JourneyDataSnapshot.StampSignal::spotCode)
				.containsExactlyElementsOf(EXPECTED_STAMPS);
	}
}
