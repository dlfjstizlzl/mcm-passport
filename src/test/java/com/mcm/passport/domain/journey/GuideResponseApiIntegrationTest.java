package com.mcm.passport.domain.journey;

import com.mcm.passport.domain.journey.entity.GuideOption;
import com.mcm.passport.domain.journey.entity.GuideQuestion;
import com.mcm.passport.domain.journey.entity.GuideResponse;
import com.mcm.passport.domain.journey.entity.JourneySpot;
import com.mcm.passport.domain.journey.repository.GuideOptionRepository;
import com.mcm.passport.domain.journey.repository.GuideQuestionRepository;
import com.mcm.passport.domain.journey.repository.GuideResponseRepository;
import com.mcm.passport.domain.journey.repository.JourneySpotRepository;
import com.mcm.passport.domain.journey.service.JourneyDataReader;
import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.passport.entity.PassportCard;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.repository.PassportCardRepository;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"mcm.style.analysis.provider=mock",
		"spring.datasource.url=jdbc:h2:mem:guide-response-api;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class GuideResponseApiIntegrationTest {

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
	private JourneyDataReader journeyDataReader;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
		guideResponseRepository.deleteAllInBatch();
		passportSessionRepository.deleteAllInBatch();
		passportCardRepository.deleteAllInBatch();
	}

	@Test
	void createsAndUpdatesSingleResponseForSessionAndQuestion() throws Exception {
		PassportSession session = saveExploringSession();
		GuideQuestion question = question("ORIGIN_GATE", "JOURNEY_START_MOOD");
		GuideOption firstOption = option(question, "REFINED_ELEGANT");
		GuideOption changedOption = option(question, "MODERN_CHIC");

		mockMvc.perform(put(endpoint(), session.getId(), question.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(optionBody(firstOption, "밤거리처럼 선명한 분위기가 좋아요")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.guideResponseId").isNumber())
				.andExpect(jsonPath("$.questionId").value(question.getId()))
				.andExpect(jsonPath("$.optionId").value(firstOption.getId()))
				.andExpect(jsonPath("$.answerText").value("밤거리처럼 선명한 분위기가 좋아요"))
				.andExpect(jsonPath("$.answeredAt").isNotEmpty());

		Long responseId = guideResponseRepository.findAll().getFirst().getId();
		JourneyDataSnapshot.ResponseSignal signal = journeyDataReader.read(session.getId()).responses().getFirst();
		assertThat(signal.answerText())
				.contains(firstOption.getLabel())
				.contains("밤거리처럼 선명한 분위기가 좋아요");

		mockMvc.perform(put(endpoint(), session.getId(), question.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(optionBody(changedOption)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.guideResponseId").value(responseId))
				.andExpect(jsonPath("$.optionId").value(changedOption.getId()));

		assertThat(guideResponseRepository.count()).isEqualTo(1);
		assertThat(guideResponseRepository.findById(responseId).orElseThrow().getGuideOption().getId())
				.isEqualTo(changedOption.getId());
		assertThat(guideResponseRepository.findById(responseId).orElseThrow().getAnswerText()).isNull();
	}

	@Test
	void createsResponseWithCustomAnswerOnly() throws Exception {
		PassportSession session = saveExploringSession();
		GuideQuestion question = question("ORIGIN_GATE", "JOURNEY_START_MOOD");

		mockMvc.perform(put(endpoint(), session.getId(), question.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"answerText\":\"도시의 밤처럼 선명한 분위기\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.optionId").isEmpty())
				.andExpect(jsonPath("$.answerText").value("도시의 밤처럼 선명한 분위기"));

		GuideResponse saved = guideResponseRepository.findAll().getFirst();
		assertThat(saved.getGuideOption()).isNull();
		assertThat(journeyDataReader.read(session.getId()).responses().getFirst().answerCode()).isEqualTo("CUSTOM");
	}

	@Test
	void rejectsOptionBelongingToDifferentQuestion() throws Exception {
		PassportSession session = saveExploringSession();
		GuideQuestion question = question("ORIGIN_GATE", "JOURNEY_START_MOOD");
		GuideQuestion anotherQuestion = question("MATERIAL_LOUNGE", "MATERIAL_SENSATION");
		GuideOption wrongOption = option(anotherQuestion, "SUBTLE_GLOSS");

		mockMvc.perform(put(endpoint(), session.getId(), question.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(optionBody(wrongOption)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_GUIDE_OPTION"));

		assertThat(guideResponseRepository.count()).isZero();
	}

	@Test
	void rejectsResponseOutsideExploringStatus() throws Exception {
		PassportSession session = saveReadyToBoardSession();
		GuideQuestion question = question("ORIGIN_GATE", "JOURNEY_START_MOOD");
		GuideOption option = option(question, "REFINED_ELEGANT");

		mockMvc.perform(put(endpoint(), session.getId(), question.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(optionBody(option)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_SESSION_STATUS"));
	}

	@Test
	void returnsNotFoundForUnknownSessionQuestionAndOption() throws Exception {
		PassportSession session = saveExploringSession();
		GuideQuestion question = question("ORIGIN_GATE", "JOURNEY_START_MOOD");
		GuideOption option = option(question, "REFINED_ELEGANT");

		mockMvc.perform(put(endpoint(), 999999L, question.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(optionBody(option)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PASSPORT_SESSION_NOT_FOUND"));

		mockMvc.perform(put(endpoint(), session.getId(), 999999L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(optionBody(option)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("GUIDE_QUESTION_NOT_FOUND"));

		mockMvc.perform(put(endpoint(), session.getId(), question.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"optionId\":999999}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("GUIDE_OPTION_NOT_FOUND"));
	}

	@Test
	void rejectsResponseWithoutOptionOrCustomAnswer() throws Exception {
		PassportSession session = saveExploringSession();
		GuideQuestion question = question("ORIGIN_GATE", "JOURNEY_START_MOOD");

		mockMvc.perform(put(endpoint(), session.getId(), question.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
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

	private GuideQuestion question(String spotCode, String questionCode) {
		JourneySpot spot = journeySpotRepository.findByCode(spotCode).orElseThrow();
		return guideQuestionRepository.findByJourneySpot_IdAndCode(spot.getId(), questionCode).orElseThrow();
	}

	private GuideOption option(GuideQuestion question, String optionCode) {
		return guideOptionRepository.findByGuideQuestion_IdAndCode(question.getId(), optionCode).orElseThrow();
	}

	private String optionBody(GuideOption option) {
		return "{\"optionId\":" + option.getId() + "}";
	}

	private String optionBody(GuideOption option, String answerText) {
		return "{\"optionId\":" + option.getId() + ",\"answerText\":\"" + answerText + "\"}";
	}

	private String endpoint() {
		return "/api/passport-sessions/{passportSessionId}/guide-responses/{questionId}";
	}
}
