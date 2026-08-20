package com.mcm.passport.domain.style;

import com.mcm.passport.domain.journey.repository.GuideOptionRepository;
import com.mcm.passport.domain.journey.repository.GuideQuestionRepository;
import com.mcm.passport.domain.journey.repository.GuideResponseRepository;
import com.mcm.passport.domain.journey.repository.JourneySpotRepository;
import com.mcm.passport.domain.journey.repository.JourneyStampRepository;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.entity.PassportSessionStatus;
import com.mcm.passport.domain.passport.repository.PassportCardRepository;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.product.repository.ProductRepository;
import com.mcm.passport.domain.product.repository.ProductTagRepository;
import com.mcm.passport.domain.style.analysis.StyleAnalysisDecision;
import com.mcm.passport.domain.style.analysis.ValidatedStyleAnalysis;
import com.mcm.passport.domain.style.dto.StyleSpotSessionResponse;
import com.mcm.passport.domain.style.entity.CityBackground;
import com.mcm.passport.domain.style.entity.CityCode;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import com.mcm.passport.domain.style.entity.StyleMood;
import com.mcm.passport.domain.style.entity.StyleSpot;
import com.mcm.passport.domain.style.entity.StyleSpotStatus;
import com.mcm.passport.domain.style.repository.JourneySouvenirRepository;
import com.mcm.passport.domain.style.repository.StylePortraitRepository;
import com.mcm.passport.domain.style.repository.StyleResultRepository;
import com.mcm.passport.domain.style.repository.StyleSpotRepository;
import com.mcm.passport.domain.style.repository.StyleSpotSessionRepository;
import com.mcm.passport.domain.style.service.StyleAnalysisPreparation;
import com.mcm.passport.domain.style.service.StyleAnalysisTransactionService;
import com.mcm.passport.domain.style.service.StyleSpotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"mcm.style.analysis.provider=mock",
		"mcm.reference-data.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:style-api-contract;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class StyleApiContractIntegrationTest {

	private static final String STYLE_SPOT_CODE = "GATE-S1";

	@Autowired private WebApplicationContext applicationContext;
	@Autowired private PassportSessionRepository passportSessionRepository;
	@Autowired private PassportCardRepository passportCardRepository;
	@Autowired private JourneySpotRepository journeySpotRepository;
	@Autowired private GuideQuestionRepository guideQuestionRepository;
	@Autowired private GuideOptionRepository guideOptionRepository;
	@Autowired private GuideResponseRepository guideResponseRepository;
	@Autowired private JourneyStampRepository journeyStampRepository;
	@Autowired private ProductRepository productRepository;
	@Autowired private ProductTagRepository productTagRepository;
	@Autowired private StyleSpotRepository styleSpotRepository;
	@Autowired private StyleSpotSessionRepository styleSpotSessionRepository;
	@Autowired private StyleResultRepository styleResultRepository;
	@Autowired private StylePortraitRepository stylePortraitRepository;
	@Autowired private JourneySouvenirRepository journeySouvenirRepository;
	@Autowired private StyleSpotService styleSpotService;
	@Autowired private StyleAnalysisTransactionService analysisTransactionService;

	private MockMvc mockMvc;
	private IntegratedStyleTestFixture fixture;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
		cleanDatabase();
		styleSpotRepository.saveAndFlush(StyleSpot.waiting(STYLE_SPOT_CODE));
		fixture = new IntegratedStyleTestFixture(
				passportCardRepository, passportSessionRepository, journeySpotRepository,
				guideQuestionRepository, guideOptionRepository, guideResponseRepository,
				journeyStampRepository, productRepository, productTagRepository
		);
	}

	@Test
	void connectIsIdempotentForItsOwnerAndHandsFinishedSpotToNextPassport() throws Exception {
		PassportSession owner = fixture.readyToBoardWithJourney(false);
		PassportSession competitor = fixture.readyToBoardWithJourney(false);

		mockMvc.perform(post("/api/style-spots/{styleSpotId}/connect", STYLE_SPOT_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(connectBody(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RESULT"));

		Long connectionId = styleSpotSessionRepository.findActiveByStyleSpotCode(STYLE_SPOT_CODE)
				.orElseThrow().getId();
		Long resultId = styleResultRepository.findByPassportSessionId(owner.getId()).orElseThrow().getId();

		mockMvc.perform(post("/api/style-spots/{styleSpotId}/connect", "  gate-s1  ")
						.contentType(MediaType.APPLICATION_JSON)
						.content(connectBody(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.styleSpotSessionId").value(connectionId))
				.andExpect(jsonPath("$.styleResult.id").value(resultId));

		assertThat(styleSpotSessionRepository.findActiveByStyleSpotCode(STYLE_SPOT_CODE))
				.hasValueSatisfying(connection -> assertThat(connection.getId()).isEqualTo(connectionId));
		assertThat(styleResultRepository.count()).isEqualTo(1);

		mockMvc.perform(post("/api/style-spots/{styleSpotId}/connect", STYLE_SPOT_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(connectBody(competitor)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.passportSessionId").value(competitor.getId()))
				.andExpect(jsonPath("$.status").value("RESULT"));
		assertThat(styleSpotSessionRepository.findById(connectionId).orElseThrow().isActive()).isFalse();
		assertThat(styleSpotSessionRepository.findActiveByStyleSpotCode(STYLE_SPOT_CODE))
				.hasValueSatisfying(connection ->
						assertThat(connection.getPassportSession().getId()).isEqualTo(competitor.getId()));
		assertThat(styleResultRepository.count()).isEqualTo(2);
		assertThat(passportSessionRepository.findById(competitor.getId()).orElseThrow().getStatus())
				.isEqualTo(PassportSessionStatus.STYLE_SPOT);
	}

	@Test
	void retriesTheSameConnectionAfterFailureAndPreservesRejectedRetryStates() throws Exception {
		PassportSession retryOwner = fixture.readyToBoardWithJourney(false);
		StyleSpotSessionResponse retryConnection = styleSpotService
				.connectSession(STYLE_SPOT_CODE, retryOwner.getId());
		StyleAnalysisPreparation failedAttempt = analysisTransactionService.prepare(STYLE_SPOT_CODE);
		analysisTransactionService.fail(failedAttempt);

		assertThat(styleSpotRepository.findById(STYLE_SPOT_CODE).orElseThrow().getStatus())
				.isEqualTo(StyleSpotStatus.CONNECTED);
		mockMvc.perform(post("/api/style-spots/{styleSpotId}/connect", STYLE_SPOT_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(connectBody(retryOwner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.styleSpotSessionId").value(retryConnection.id()))
				.andExpect(jsonPath("$.status").value("RESULT"));
		assertThat(styleSpotSessionRepository.findActiveByStyleSpotCode(STYLE_SPOT_CODE))
				.hasValueSatisfying(connection -> assertThat(connection.getId()).isEqualTo(retryConnection.id()));
		styleSpotService.disconnect(retryConnection.id());

		PassportSession missingJourney = fixture.readyToBoardWithoutJourney();
		mockMvc.perform(post("/api/style-spots/{styleSpotId}/connect", STYLE_SPOT_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(connectBody(missingJourney)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("JOURNEY_NOT_COMPLETED"));
		Long missingJourneyConnectionId = styleSpotSessionRepository.findActiveByStyleSpotCode(STYLE_SPOT_CODE)
				.orElseThrow().getId();
		mockMvc.perform(post("/api/style-spots/{styleSpotId}/connect", STYLE_SPOT_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(connectBody(missingJourney)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("JOURNEY_NOT_COMPLETED"));
		assertThat(styleSpotRepository.findById(STYLE_SPOT_CODE).orElseThrow().getStatus())
				.isEqualTo(StyleSpotStatus.CONNECTED);
		assertThat(styleSpotSessionRepository.findActiveByStyleSpotCode(STYLE_SPOT_CODE))
				.hasValueSatisfying(connection -> assertThat(connection.getId()).isEqualTo(missingJourneyConnectionId));
		styleSpotService.disconnect(missingJourneyConnectionId);

		PassportSession analyzingOwner = fixture.readyToBoardWithJourney(false);
		StyleSpotSessionResponse analyzingConnection = styleSpotService
				.connectSession(STYLE_SPOT_CODE, analyzingOwner.getId());
		StyleAnalysisPreparation analyzingAttempt = analysisTransactionService.prepare(STYLE_SPOT_CODE);

		mockMvc.perform(post("/api/style-spots/{styleSpotId}/connect", STYLE_SPOT_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(connectBody(analyzingOwner)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_SESSION_STATUS"));
		assertThat(styleSpotRepository.findById(STYLE_SPOT_CODE).orElseThrow().getStatus())
				.isEqualTo(StyleSpotStatus.ANALYZING);
		assertThat(styleSpotSessionRepository.findActiveByStyleSpotCode(STYLE_SPOT_CODE))
				.hasValueSatisfying(connection -> assertThat(connection.getId()).isEqualTo(analyzingConnection.id()));

		analysisTransactionService.fail(analyzingAttempt);
		styleSpotService.disconnect(analyzingConnection.id());
	}

	@Test
	void displayUsesOneFlatNullableShapeForAllFiveStates() throws Exception {
		PassportSession session = fixture.readyToBoardWithJourney(false);

		assertDisplayWithoutResult("WAITING", null);

		StyleSpotSessionResponse connection = styleSpotService.connectSession(STYLE_SPOT_CODE, session.getId());
		assertDisplayWithoutResult("CONNECTED", connection.id());

		StyleAnalysisPreparation preparation = analysisTransactionService.prepare(STYLE_SPOT_CODE);
		assertDisplayWithoutResult("ANALYZING", connection.id());

		Long resultId = analysisTransactionService.complete(preparation, validDecision()).id();
		mockMvc.perform(get("/api/style-spots/{styleSpotId}/display", STYLE_SPOT_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", aMapWithSize(4)))
				.andExpect(jsonPath("$.styleSpotId").value(STYLE_SPOT_CODE))
				.andExpect(jsonPath("$.activeStyleSpotSessionId").value(connection.id()))
				.andExpect(jsonPath("$.status").value("RESULT"))
				.andExpect(jsonPath("$.styleResult.id").value(resultId));

		styleSpotService.disconnect(connection.id());
		assertDisplayWithoutResult("RESET", null);
	}

	@Test
	void souvenirCreationIsIdempotentAndPreservesEmptyOptionalProductTags() throws Exception {
		PassportSession session = fixture.readyToBoardWithJourney(false);
		connect(session);

		mockMvc.perform(post(
					"/api/passport-sessions/{passportSessionId}/souvenir", session.getId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.taggedProductCodes").isArray())
				.andExpect(jsonPath("$.taggedProductCodes").isEmpty())
				.andExpect(jsonPath("$.journeyStamps.length()").value(4));
		Long souvenirId = journeySouvenirRepository.findByPassportSessionId(session.getId())
				.orElseThrow().getId();

		mockMvc.perform(post("/api/passport-sessions/{passportSessionId}/souvenir", session.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(souvenirId))
				.andExpect(jsonPath("$.passportSessionId").value(session.getId()))
				.andExpect(jsonPath("$.taggedProductCodes").isEmpty());
		mockMvc.perform(get("/api/passport-sessions/{passportSessionId}/souvenir", session.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(souvenirId))
				.andExpect(jsonPath("$.passportSessionId").value(session.getId()))
				.andExpect(jsonPath("$.taggedProductCodes").isEmpty());

		assertThat(journeySouvenirRepository.count()).isEqualTo(1);
		assertThat(passportSessionRepository.findById(session.getId()).orElseThrow().getStatus())
				.isEqualTo(PassportSessionStatus.COMPLETED);
	}

	@Test
	void myPassportExposesPartialResultAndCompletedAggregatesWithExplicitNulls() throws Exception {
		PassportSession session = fixture.readyToBoardWithJourney(false);

		mockMvc.perform(get("/api/passport-sessions/{passportSessionId}/my-passport", session.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", aMapWithSize(9)))
				.andExpect(jsonPath("$.status").value("READY_TO_BOARD"))
				.andExpect(jsonPath("$.completedAt").value(nullValue()))
				.andExpect(jsonPath("$.journey.responses.length()").value(2))
				.andExpect(jsonPath("$.journey.stamps.length()").value(4))
				.andExpect(jsonPath("$.journey.taggedProducts").isEmpty())
				.andExpect(jsonPath("$.styleResult").value(nullValue()))
				.andExpect(jsonPath("$.portrait").value(nullValue()))
				.andExpect(jsonPath("$.souvenir").value(nullValue()));

		connect(session);
		mockMvc.perform(get("/api/passport-sessions/{passportSessionId}/my-passport", session.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("STYLE_SPOT"))
				.andExpect(jsonPath("$.completedAt").value(nullValue()))
				.andExpect(jsonPath("$.styleResult.id").isNumber())
				.andExpect(jsonPath("$.portrait").value(nullValue()))
				.andExpect(jsonPath("$.souvenir").value(nullValue()));

		mockMvc.perform(post("/api/passport-sessions/{passportSessionId}/souvenir", session.getId()))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/passport-sessions/{passportSessionId}/my-passport", session.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.completedAt").isString())
				.andExpect(jsonPath("$.styleResult.id").isNumber())
				.andExpect(jsonPath("$.portrait").value(nullValue()))
				.andExpect(jsonPath("$.souvenir.id").isNumber());
	}

	@Test
	void validationErrorsUseTheDocumentedErrorResponseShape() throws Exception {
		mockMvc.perform(post("/api/style-spots/{styleSpotId}/connect", STYLE_SPOT_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$", aMapWithSize(6)))
				.andExpect(jsonPath("$.timestamp").isString())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.message", not(emptyOrNullString())))
				.andExpect(jsonPath("$.path").value("/api/style-spots/GATE-S1/connect"))
				.andExpect(jsonPath("$.errors.length()").value(1))
				.andExpect(jsonPath("$.errors[0]", aMapWithSize(2)))
				.andExpect(jsonPath("$.errors[0].field").value("passportSessionId"))
				.andExpect(jsonPath("$.errors[0].reason", not(emptyOrNullString())));
	}

	private void connect(PassportSession session) throws Exception {
		mockMvc.perform(post("/api/style-spots/{styleSpotId}/connect", STYLE_SPOT_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(connectBody(session)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RESULT"));
	}

	private String connectBody(PassportSession session) {
		return "{\"passportSessionId\":" + session.getId() + "}";
	}

	private void assertDisplayWithoutResult(String statusValue, Long connectionId) throws Exception {
		var result = mockMvc.perform(get("/api/style-spots/{styleSpotId}/display", STYLE_SPOT_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", aMapWithSize(4)))
				.andExpect(jsonPath("$.styleSpotId").value(STYLE_SPOT_CODE))
				.andExpect(jsonPath("$.status").value(statusValue))
				.andExpect(jsonPath("$.styleResult").value(nullValue()));
		if (connectionId == null) {
			result.andExpect(jsonPath("$.activeStyleSpotSessionId").value(nullValue()));
		} else {
			result.andExpect(jsonPath("$.activeStyleSpotSessionId").value(connectionId));
		}
	}

	private StyleAnalysisDecision validDecision() {
		return new StyleAnalysisDecision(
				new ValidatedStyleAnalysis(
						CityCode.BERLIN_AFTERDARK_NOMAD,
						RecommendedProduct.STARK_BACKPACK,
						StyleMood.AFTERDARK_MOVEMENT,
						CityBackground.BERLIN_AFTER_DARK,
						"A valid API contract test analysis result.",
						92
				),
				false
		);
	}

	private void cleanDatabase() {
		stylePortraitRepository.deleteAllInBatch();
		journeySouvenirRepository.deleteAll();
		journeySouvenirRepository.flush();
		styleResultRepository.deleteAllInBatch();
		styleSpotSessionRepository.deleteAllInBatch();
		styleSpotRepository.deleteAllInBatch();
		productTagRepository.deleteAllInBatch();
		guideResponseRepository.deleteAllInBatch();
		journeyStampRepository.deleteAllInBatch();
		passportSessionRepository.deleteAllInBatch();
		passportCardRepository.deleteAllInBatch();
	}
}
