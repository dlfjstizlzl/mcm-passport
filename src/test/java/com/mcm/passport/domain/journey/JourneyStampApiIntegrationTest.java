package com.mcm.passport.domain.journey;

import com.mcm.passport.domain.journey.entity.GuideOption;
import com.mcm.passport.domain.journey.entity.GuideQuestion;
import com.mcm.passport.domain.journey.entity.GuideResponse;
import com.mcm.passport.domain.journey.entity.JourneySpot;
import com.mcm.passport.domain.journey.repository.GuideOptionRepository;
import com.mcm.passport.domain.journey.repository.GuideQuestionRepository;
import com.mcm.passport.domain.journey.repository.GuideResponseRepository;
import com.mcm.passport.domain.journey.repository.JourneySpotRepository;
import com.mcm.passport.domain.journey.repository.JourneyStampRepository;
import com.mcm.passport.domain.passport.entity.PassportCard;
import com.mcm.passport.domain.passport.entity.PassportSession;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"mcm.style.analysis.provider=mock",
		"spring.datasource.url=jdbc:h2:mem:journey-stamp-api;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class JourneyStampApiIntegrationTest {

	@Autowired
	private WebApplicationContext applicationContext;

	@Autowired
	private PassportCardRepository passportCardRepository;

	@Autowired
	private PassportSessionRepository passportSessionRepository;

	@Autowired
	private JourneySpotRepository journeySpotRepository;

	@Autowired
	private GuideQuestionRepository guideQuestionRepository;

	@Autowired
	private GuideOptionRepository guideOptionRepository;

	@Autowired
	private GuideResponseRepository guideResponseRepository;

	@Autowired
	private JourneyStampRepository journeyStampRepository;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
		journeyStampRepository.deleteAllInBatch();
		guideResponseRepository.deleteAllInBatch();
		passportSessionRepository.deleteAllInBatch();
		passportCardRepository.deleteAllInBatch();
	}

	@Test
	void createsStampWhenAllRequiredQuestionsAreAnswered() throws Exception {
		PassportSession session = saveExploringSession();
		JourneySpot spot = spot("ORIGIN_GATE");
		answerAllRequiredQuestions(session, spot);

		mockMvc.perform(post(endpoint(), session.getId(), spot.getId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.journeyStampId").isNumber())
				.andExpect(jsonPath("$.journeySpotId").value(spot.getId()))
				.andExpect(jsonPath("$.stampedAt").isNotEmpty());

		assertThat(journeyStampRepository.count()).isEqualTo(1);
		assertThat(journeyStampRepository.findAll().getFirst().getJourneySpot().getId()).isEqualTo(spot.getId());
	}

	@Test
	void rejectsStampWhenRequiredQuestionIsNotAnswered() throws Exception {
		PassportSession session = saveExploringSession();
		JourneySpot spot = spot("ORIGIN_GATE");

		mockMvc.perform(post(endpoint(), session.getId(), spot.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("JOURNEY_NOT_COMPLETED"));

		assertThat(journeyStampRepository.count()).isZero();
	}

	@Test
	void rejectsDuplicateStamp() throws Exception {
		PassportSession session = saveExploringSession();
		JourneySpot spot = spot("ORIGIN_GATE");
		answerAllRequiredQuestions(session, spot);

		mockMvc.perform(post(endpoint(), session.getId(), spot.getId()))
				.andExpect(status().isCreated());
		mockMvc.perform(post(endpoint(), session.getId(), spot.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("JOURNEY_STAMP_ALREADY_EXISTS"));

		assertThat(journeyStampRepository.count()).isEqualTo(1);
	}

	@Test
	void rejectsStampOutsideExploringStatus() throws Exception {
		PassportSession session = saveReadyToBoardSession();
		JourneySpot spot = spot("ORIGIN_GATE");

		mockMvc.perform(post(endpoint(), session.getId(), spot.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_SESSION_STATUS"));
	}

	@Test
	void returnsNotFoundForUnknownSessionAndSpot() throws Exception {
		PassportSession session = saveExploringSession();
		JourneySpot spot = spot("ORIGIN_GATE");

		mockMvc.perform(post(endpoint(), 999999L, spot.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PASSPORT_SESSION_NOT_FOUND"));

		mockMvc.perform(post(endpoint(), session.getId(), 999999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("JOURNEY_SPOT_NOT_FOUND"));
	}

	private void answerAllRequiredQuestions(PassportSession session, JourneySpot spot) {
		List<GuideQuestion> questions = guideQuestionRepository
				.findAllByJourneySpot_IdAndRequiredTrueAndActiveTrueOrderBySequenceAsc(spot.getId());
		List<GuideResponse> responses = questions.stream()
				.map(question -> GuideResponse.create(session, question, firstOption(question)))
				.toList();
		guideResponseRepository.saveAllAndFlush(responses);
	}

	private GuideOption firstOption(GuideQuestion question) {
		return guideOptionRepository
				.findAllByGuideQuestion_IdAndActiveTrueOrderBySequenceAsc(question.getId())
				.getFirst();
	}

	private JourneySpot spot(String code) {
		return journeySpotRepository.findByCode(code).orElseThrow();
	}

	private PassportSession saveExploringSession() {
		PassportCard card = saveCard();
		return passportSessionRepository.saveAndFlush(PassportSession.start(card));
	}

	private PassportSession saveReadyToBoardSession() {
		PassportCard card = saveCard();
		return passportSessionRepository.saveAndFlush(PassportSession.readyToBoard(card));
	}

	private PassportCard saveCard() {
		return passportCardRepository.saveAndFlush(PassportCard.issue("TEST-" + UUID.randomUUID()));
	}

	private String endpoint() {
		return "/api/passport-sessions/{passportSessionId}/journey-spots/{journeySpotId}/complete";
	}
}
