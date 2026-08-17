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
	void prototypePolicyAcceptsResponsesAndStampsWithOptionalTaggedProducts() {
		assertThatCode(() -> policy.validateForAnalysis(completeJourneyData()))
				.doesNotThrowAnyException();
	}

	@Test
	void prototypePolicyAcceptsResponsesAndStampsWithoutTaggedProducts() {
		JourneyDataSnapshot complete = completeJourneyData();
		JourneyDataSnapshot withoutProductTag = new JourneyDataSnapshot(
				complete.sessionId(),
				complete.responses(),
				complete.stamps(),
				List.of()
		);

		assertThatCode(() -> policy.validateForAnalysis(withoutProductTag))
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsEmptyJourneyData() {
		JourneyDataSnapshot empty = new JourneyDataSnapshot(1L, List.of(), List.of(), List.of());

		assertJourneyNotCompleted(empty);
	}

	@Test
	void prototypePolicyStillRequiresResponseSignals() {
		JourneyDataSnapshot complete = completeJourneyData();
		JourneyDataSnapshot withoutResponses = new JourneyDataSnapshot(
				complete.sessionId(),
				List.of(),
				complete.stamps(),
				complete.taggedProducts()
		);

		assertJourneyNotCompleted(withoutResponses);
	}

	@Test
	void prototypePolicyStillRequiresJourneyProgressSignals() {
		JourneyDataSnapshot complete = completeJourneyData();
		JourneyDataSnapshot withoutStamps = new JourneyDataSnapshot(
				complete.sessionId(),
				complete.responses(),
				List.of(),
				complete.taggedProducts()
		);

		assertJourneyNotCompleted(withoutStamps);
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
