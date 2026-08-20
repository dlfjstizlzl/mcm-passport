package com.mcm.passport.domain.passport;

import com.mcm.passport.domain.passport.entity.PassportCard;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"mcm.style.analysis.provider=mock",
		"mcm.passport.allow-virtual-cards=true",
		"spring.datasource.url=jdbc:h2:mem:passport-session-api;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class PassportSessionApiIntegrationTest {

	@Autowired
	private WebApplicationContext applicationContext;

	@Autowired
	private PassportCardRepository passportCardRepository;

	@Autowired
	private PassportSessionRepository passportSessionRepository;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
		passportSessionRepository.deleteAllInBatch();
		passportCardRepository.deleteAllInBatch();
	}

	@Test
	void createsAndGetsPassportSession() throws Exception {
		passportCardRepository.saveAndFlush(PassportCard.issue("MCM-PASSPORT-001"));

		mockMvc.perform(post("/api/passport-sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"cardUid\":\"MCM-PASSPORT-001\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.passportSessionId").isNumber())
				.andExpect(jsonPath("$.status").value("EXPLORING"))
				.andExpect(jsonPath("$.startedAt").isNotEmpty())
				.andExpect(jsonPath("$.completedAt").doesNotExist());

		Long sessionId = passportSessionRepository.findAll().getFirst().getId();
		mockMvc.perform(get("/api/passport-sessions/{passportSessionId}", sessionId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.passportSessionId").value(sessionId))
				.andExpect(jsonPath("$.status").value("EXPLORING"))
				.andExpect(jsonPath("$.startedAt").isNotEmpty());
	}

	@Test
	void rejectsDuplicateActiveSessionForSameCard() throws Exception {
		passportCardRepository.saveAndFlush(PassportCard.issue("MCM-PASSPORT-002"));
		String request = "{\"cardUid\":\"MCM-PASSPORT-002\"}";

		mockMvc.perform(post("/api/passport-sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/passport-sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ACTIVE_PASSPORT_SESSION_EXISTS"));
	}

	@Test
	void createsIndependentVirtualBrowserCardSession() throws Exception {
		mockMvc.perform(post("/api/passport-sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"cardUid\":\"MCM-WEB-BROWSER-A\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("EXPLORING"));
	}

	@Test
	void rejectsInactiveCard() throws Exception {
		PassportCard passportCard = PassportCard.issue("MCM-PASSPORT-003");
		passportCard.deactivate();
		passportCardRepository.saveAndFlush(passportCard);

		mockMvc.perform(post("/api/passport-sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"cardUid\":\"MCM-PASSPORT-003\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PASSPORT_CARD_INACTIVE"));
	}

	@Test
	void returnsNotFoundForUnknownCardAndSession() throws Exception {
		mockMvc.perform(post("/api/passport-sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"cardUid\":\"UNKNOWN\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PASSPORT_CARD_NOT_FOUND"));

		mockMvc.perform(get("/api/passport-sessions/{passportSessionId}", 999999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PASSPORT_SESSION_NOT_FOUND"));
	}

	@Test
	void validatesRequiredCardUid() throws Exception {
		mockMvc.perform(post("/api/passport-sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"cardUid\":\" \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[0].field").value("cardUid"));
	}
}
