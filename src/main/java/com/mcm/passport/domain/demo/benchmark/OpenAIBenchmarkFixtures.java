package com.mcm.passport.domain.demo.benchmark;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import java.util.List;

public final class OpenAIBenchmarkFixtures {

	private static final long FIXTURE_SESSION_ID = 1L;

	private static final List<JourneyDataSnapshot.ResponseSignal> RESPONSES = List.of(
			new JourneyDataSnapshot.ResponseSignal(
					"CITY_MOOD_ROOM",
					"TODAY_MOOD",
					"AFTERDARK",
					"Afterdark movement"
			),
			new JourneyDataSnapshot.ResponseSignal(
					"MOVEMENT_DECK",
					"PACE",
					"DYNAMIC",
					"Dynamic movement"
			)
	);

	private static final List<JourneyDataSnapshot.StampSignal> STAMPS = List.of(
			new JourneyDataSnapshot.StampSignal("ORIGIN_GATE"),
			new JourneyDataSnapshot.StampSignal("MATERIAL_LOUNGE"),
			new JourneyDataSnapshot.StampSignal("MOVEMENT_DECK"),
			new JourneyDataSnapshot.StampSignal("CITY_MOOD_ROOM")
	);

	private static final List<OpenAIBenchmarkCase> CASES = List.of(
			new OpenAIBenchmarkCase(
					"CASE_A_WITHOUT_PRODUCT_TAG",
					new JourneyDataSnapshot(FIXTURE_SESSION_ID, RESPONSES, STAMPS, List.of())
			),
			new OpenAIBenchmarkCase(
					"CASE_B_WITH_PRODUCT_TAG",
					new JourneyDataSnapshot(
							FIXTURE_SESSION_ID,
							RESPONSES,
							STAMPS,
							List.of(new JourneyDataSnapshot.ProductSignal(
									1L,
									RecommendedProduct.STARK_BACKPACK.name(),
									RecommendedProduct.STARK_BACKPACK.getDisplayName()
							))
					)
			)
	);
	private static final List<String> CASE_NAMES = CASES.stream()
			.map(OpenAIBenchmarkCase::name)
			.toList();

	private OpenAIBenchmarkFixtures() {
	}

	public static List<OpenAIBenchmarkCase> cases() {
		return CASES;
	}

	public static List<String> caseNames() {
		return CASE_NAMES;
	}
}
