package com.mcm.passport.domain.boarding;

import com.mcm.passport.domain.boarding.repository.BoardingPassRepository;
import com.mcm.passport.domain.journey.entity.JourneySpot;
import com.mcm.passport.domain.journey.entity.JourneyStamp;
import com.mcm.passport.domain.journey.repository.JourneySpotRepository;
import com.mcm.passport.domain.journey.repository.JourneyStampRepository;
import com.mcm.passport.domain.passport.entity.PassportCard;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.entity.PassportSessionStatus;
import com.mcm.passport.domain.passport.repository.PassportCardRepository;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"mcm.style.analysis.provider=mock",
		"spring.datasource.url=jdbc:h2:mem:boarding-pass-api;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class BoardingPassApiIntegrationTest {
	@Autowired private WebApplicationContext applicationContext;
	@Autowired private PassportCardRepository passportCardRepository;
	@Autowired private PassportSessionRepository passportSessionRepository;
	@Autowired private JourneySpotRepository journeySpotRepository;
	@Autowired private JourneyStampRepository journeyStampRepository;
	@Autowired private BoardingPassRepository boardingPassRepository;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
		boardingPassRepository.deleteAllInBatch();
		journeyStampRepository.deleteAllInBatch();
		passportSessionRepository.deleteAllInBatch();
		passportCardRepository.deleteAllInBatch();
	}

	@Test
	void issuesAndGetsBoardingPassAfterRequiredSpotsAreCompleted() throws Exception {
		PassportSession session = saveExploringSession();
		stampAllRequiredSpots(session);

		mockMvc.perform(post(boardingPassEndpoint(), session.getId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.boardingPassId").isNumber())
				.andExpect(jsonPath("$.passportSessionId").value(session.getId()))
				.andExpect(jsonPath("$.gate").value("STYLE_SPOT"))
				.andExpect(jsonPath("$.issuedAt").isNotEmpty());
		mockMvc.perform(get(boardingPassEndpoint(), session.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.boardingPassId").isNumber())
				.andExpect(jsonPath("$.passportSessionId").doesNotExist())
				.andExpect(jsonPath("$.gate").value("STYLE_SPOT"));

		assertThat(passportSessionRepository.findById(session.getId()).orElseThrow().getStatus())
				.isEqualTo(PassportSessionStatus.READY_TO_BOARD);
		assertThat(boardingPassRepository.count()).isEqualTo(1);
	}

	@Test
	void rejectsIssueWhenRequiredJourneyIsIncomplete() throws Exception {
		PassportSession session = saveExploringSession();

		mockMvc.perform(post(boardingPassEndpoint(), session.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("JOURNEY_NOT_COMPLETED"));
		assertThat(boardingPassRepository.count()).isZero();
	}

	@Test
	void rejectsIssueOutsideExploringAndReturnsNotFound() throws Exception {
		PassportSession session = saveReadyToBoardSession();

		mockMvc.perform(post(boardingPassEndpoint(), session.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_SESSION_STATUS"));
		mockMvc.perform(get(boardingPassEndpoint(), session.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("BOARDING_PASS_NOT_FOUND"));
		mockMvc.perform(get(boardingPassEndpoint(), 999999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PASSPORT_SESSION_NOT_FOUND"));
	}

	@Test
	void getsJourneyProgressWithCompletionAndStampedAt() throws Exception {
		PassportSession session = saveExploringSession();
		List<JourneySpot> spots = journeySpotRepository.findAllByActiveTrueOrderBySequenceAsc();
		JourneySpot completedSpot = spots.getFirst();
		journeyStampRepository.saveAndFlush(JourneyStamp.create(session, completedSpot));

		mockMvc.perform(get("/api/passport-sessions/{passportSessionId}/journey", session.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.passportSessionId").value(session.getId()))
				.andExpect(jsonPath("$.status").value("EXPLORING"))
				.andExpect(jsonPath("$.spots.length()").value(spots.size()))
				.andExpect(jsonPath("$.journeySpots").doesNotExist())
				.andExpect(jsonPath("$.spots[0].id").value(completedSpot.getId()))
				.andExpect(jsonPath("$.spots[0].journeySpotId").doesNotExist())
				.andExpect(jsonPath("$.spots[0].description").doesNotExist())
				.andExpect(jsonPath("$.spots[0].completed").value(true))
				.andExpect(jsonPath("$.spots[0].stampedAt").isNotEmpty())
				.andExpect(jsonPath("$.spots[1].completed").value(false))
				.andExpect(jsonPath("$.spots[1].stampedAt").doesNotExist());
	}

	@Test
	void rejectsDuplicateBoardingPassBeforeSessionStatusValidation() throws Exception {
		PassportSession session = saveExploringSession();
		stampAllRequiredSpots(session);
		mockMvc.perform(post(boardingPassEndpoint(), session.getId())).andExpect(status().isCreated());

		mockMvc.perform(post(boardingPassEndpoint(), session.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BOARDING_PASS_ALREADY_EXISTS"));
	}

	private void stampAllRequiredSpots(PassportSession session) {
		var stamps = journeySpotRepository.findAllByRequiredTrueAndActiveTrueOrderBySequenceAsc().stream()
				.map(spot -> JourneyStamp.create(session, spot)).toList();
		journeyStampRepository.saveAllAndFlush(stamps);
	}

	private PassportSession saveExploringSession() {
		return passportSessionRepository.saveAndFlush(PassportSession.start(saveCard()));
	}

	private PassportSession saveReadyToBoardSession() {
		return passportSessionRepository.saveAndFlush(PassportSession.readyToBoard(saveCard()));
	}

	private PassportCard saveCard() {
		return passportCardRepository.saveAndFlush(PassportCard.issue("TEST-" + UUID.randomUUID()));
	}

	private String boardingPassEndpoint() {
		return "/api/passport-sessions/{passportSessionId}/boarding-pass";
	}
}
