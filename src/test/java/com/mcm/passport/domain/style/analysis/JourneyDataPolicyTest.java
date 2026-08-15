package com.mcm.passport.domain.style.analysis;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JourneyDataPolicyTest {

	private final JourneyDataPolicy policy = new JourneyDataPolicy();

	@Test
	void acceptsSnapshotWithResponsesStampsAndTaggedProducts() {
		assertThatCode(() -> policy.validateForAnalysis(completeJourneyData()))
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsEmptyJourneyData() {
		JourneyDataSnapshot empty = new JourneyDataSnapshot(1L, List.of(), List.of(), List.of());

		assertJourneyNotCompleted(empty);
	}

	@Test
	void rejectsJourneyWithoutTaggedProducts() {
		JourneyDataSnapshot complete = completeJourneyData();
		JourneyDataSnapshot missingProduct = new JourneyDataSnapshot(
				complete.sessionId(),
				complete.responses(),
				complete.stamps(),
				List.of()
		);

		assertJourneyNotCompleted(missingProduct);
	}

	private JourneyDataSnapshot completeJourneyData() {
		return new JourneyDataSnapshot(
				1L,
				List.of(new JourneyDataSnapshot.ResponseSignal(
						"CITY_MOOD_ROOM",
						"MOOD",
						"AFTERDARK",
						"Afterdark"
				)),
				List.of(new JourneyDataSnapshot.StampSignal("CITY_MOOD_ROOM")),
				List.of(new JourneyDataSnapshot.ProductSignal(10L, "STARK_BACKPACK", "Stark Backpack"))
		);
	}

	private void assertJourneyNotCompleted(JourneyDataSnapshot journeyData) {
		assertThatThrownBy(() -> policy.validateForAnalysis(journeyData))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.JOURNEY_NOT_COMPLETED));
	}
}
