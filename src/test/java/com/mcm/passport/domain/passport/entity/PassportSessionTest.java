package com.mcm.passport.domain.passport.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PassportSessionTest {

	@Test
	void startsExploringSessionForPassportCard() {
		PassportCard passportCard = PassportCard.issue("MCM-PASSPORT-001");

		PassportSession session = PassportSession.start(passportCard);

		assertThat(session.getPassportCard()).isSameAs(passportCard);
		assertThat(session.getStatus()).isEqualTo(PassportSessionStatus.EXPLORING);
		assertThat(session.getStartedAt()).isNotNull();
		assertThat(session.getCreatedAt()).isNotNull();
		assertThat(session.getUpdatedAt()).isNotNull();
		assertThat(session.getCompletedAt()).isNull();
	}

	@Test
	void recordsCompletionTimeAfterValidStateTransitions() {
		PassportSession session = PassportSession.start(PassportCard.issue("MCM-PASSPORT-002"));

		session.markReadyToBoard();
		session.enterStyleSpot();
		session.complete();

		assertThat(session.getStatus()).isEqualTo(PassportSessionStatus.COMPLETED);
		assertThat(session.getCompletedAt()).isNotNull();
	}
}
