package com.mcm.passport.domain.style;

import com.mcm.passport.domain.boarding.repository.BoardingPassRepository;
import com.mcm.passport.domain.journey.entity.GuideQuestion;
import com.mcm.passport.domain.journey.entity.JourneySpot;
import com.mcm.passport.domain.journey.repository.GuideOptionRepository;
import com.mcm.passport.domain.journey.repository.GuideQuestionRepository;
import com.mcm.passport.domain.journey.repository.GuideResponseRepository;
import com.mcm.passport.domain.journey.repository.JourneySpotRepository;
import com.mcm.passport.domain.journey.repository.JourneyStampRepository;
import com.mcm.passport.domain.passport.entity.PassportCard;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.entity.PassportSessionStatus;
import com.mcm.passport.domain.passport.repository.PassportCardRepository;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.product.repository.ProductRepository;
import com.mcm.passport.domain.product.repository.ProductTagRepository;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import com.mcm.passport.domain.style.entity.StyleSpot;
import com.mcm.passport.domain.style.repository.JourneySouvenirRepository;
import com.mcm.passport.domain.style.repository.StylePortraitRepository;
import com.mcm.passport.domain.style.repository.StyleResultRepository;
import com.mcm.passport.domain.style.repository.StyleSpotRepository;
import com.mcm.passport.domain.style.repository.StyleSpotSessionRepository;
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
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"mcm.style.analysis.provider=mock",
		"mcm.reference-data.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:style-api-integrated;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class StyleApiIntegrationTest {

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
	@Autowired private BoardingPassRepository boardingPassRepository;

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
	void exposesOfficialIntegratedStyleResultPortraitSouvenirAndMyPassportApis() throws Exception {
		PassportSession session = fixture.readyToBoardWithJourney(true);

		mockMvc.perform(post("/api/style-spots/{styleSpotId}/connect", STYLE_SPOT_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"passportSessionId\":" + session.getId() + "}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.styleSpotSessionId").isNumber())
				.andExpect(jsonPath("$.styleSpotId").value(STYLE_SPOT_CODE))
				.andExpect(jsonPath("$.passportSessionId").value(session.getId()))
				.andExpect(jsonPath("$.status").value("RESULT"))
				.andExpect(jsonPath("$.styleResult.cityCode").value("BERLIN_AFTERDARK_NOMAD"))
				.andExpect(jsonPath("$.styleResult.usedFallback").value(false));

		Long connectionId = styleSpotSessionRepository.findActiveByStyleSpotCode(STYLE_SPOT_CODE)
				.orElseThrow().getId();

		mockMvc.perform(get("/api/style-spots/{styleSpotId}/display", STYLE_SPOT_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activeStyleSpotSessionId").value(connectionId))
				.andExpect(jsonPath("$.status").value("RESULT"))
				.andExpect(jsonPath("$.styleResult.passportSessionId").value(session.getId()));

		mockMvc.perform(get("/api/passport-sessions/{passportSessionId}/style-result", session.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recommendedProductCode").value(RecommendedProduct.STARK_BACKPACK.name()));

		String portraitBody = "{\"imageUrl\":\"https://example.com/portrait.jpg\",\"consent\":true}";
		mockMvc.perform(post("/api/passport-sessions/{passportSessionId}/portrait", session.getId())
						.contentType(MediaType.APPLICATION_JSON).content(portraitBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.passportSessionId").value(session.getId()))
				.andExpect(jsonPath("$.consent").value(true));
		mockMvc.perform(get("/api/passport-sessions/{passportSessionId}/portrait", session.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.imageUrl").value("https://example.com/portrait.jpg"));

		mockMvc.perform(get("/api/passport-sessions/{passportSessionId}/my-passport", session.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("STYLE_SPOT"))
				.andExpect(jsonPath("$.journey.responses.length()").value(2))
				.andExpect(jsonPath("$.journey.taggedProducts[0].code")
						.value(RecommendedProduct.STARK_BACKPACK.name()))
				.andExpect(jsonPath("$.portrait.id").isNumber())
				.andExpect(jsonPath("$.souvenir").doesNotExist());

		mockMvc.perform(delete("/api/passport-sessions/{passportSessionId}/portrait", session.getId()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/passport-sessions/{passportSessionId}/portrait", session.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("STYLE_PORTRAIT_NOT_FOUND"));

		mockMvc.perform(post("/api/passport-sessions/{passportSessionId}/souvenir", session.getId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.journeyStamps", hasItem("CITY_MOOD_ROOM")))
				.andExpect(jsonPath("$.taggedProductCodes", hasItem(RecommendedProduct.STARK_BACKPACK.name())));

		mockMvc.perform(get("/api/passport-sessions/{passportSessionId}/my-passport", session.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.portrait").doesNotExist())
				.andExpect(jsonPath("$.souvenir.id").isNumber());

		mockMvc.perform(post("/api/style-spot-sessions/{styleSpotSessionId}/disconnect", connectionId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DISCONNECTED"));
	}

	@Test
	void validatesOfficialConnectRequestAndMissingResult() throws Exception {
		mockMvc.perform(post("/api/style-spots/{styleSpotId}/connect", STYLE_SPOT_CODE)
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));

		mockMvc.perform(get("/api/passport-sessions/{passportSessionId}/style-result", 999999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("STYLE_RESULT_NOT_FOUND"));
	}

	@Test
	void flowsFromBe1JourneyApisThroughBoardingAndOfficialStyleConnect() throws Exception {
		PassportCard card = passportCardRepository.saveAndFlush(
				PassportCard.issue("FULL-FLOW-" + UUID.randomUUID())
		);
		PassportSession session = passportSessionRepository.saveAndFlush(PassportSession.start(card));

		for (JourneySpot spot : journeySpotRepository.findAllByRequiredTrueAndActiveTrueOrderBySequenceAsc()) {
			for (GuideQuestion question : guideQuestionRepository
					.findAllByJourneySpot_IdAndRequiredTrueAndActiveTrueOrderBySequenceAsc(spot.getId())) {
				Long optionId = guideOptionRepository
						.findAllByGuideQuestion_IdAndActiveTrueOrderBySequenceAsc(question.getId())
						.getFirst().getId();
				mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
								"/api/passport-sessions/{passportSessionId}/guide-responses/{questionId}",
								session.getId(), question.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"optionId\":" + optionId + "}"))
						.andExpect(status().isOk());
			}
			mockMvc.perform(post(
						"/api/passport-sessions/{passportSessionId}/journey-spots/{journeySpotId}/complete",
						session.getId(), spot.getId()))
					.andExpect(status().isCreated());
		}

		mockMvc.perform(post("/api/passport-sessions/{passportSessionId}/boarding-pass", session.getId()))
				.andExpect(status().isCreated());
		assertThat(passportSessionRepository.findById(session.getId()).orElseThrow().getStatus())
				.isEqualTo(PassportSessionStatus.READY_TO_BOARD);

		mockMvc.perform(post("/api/style-spots/{styleSpotId}/connect", STYLE_SPOT_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"passportSessionId\":" + session.getId() + "}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RESULT"))
				.andExpect(jsonPath("$.styleResult.passportSessionId").value(session.getId()));
	}

	private void cleanDatabase() {
		stylePortraitRepository.deleteAllInBatch();
		journeySouvenirRepository.deleteAll();
		journeySouvenirRepository.flush();
		styleResultRepository.deleteAllInBatch();
		styleSpotSessionRepository.deleteAllInBatch();
		styleSpotRepository.deleteAllInBatch();
		boardingPassRepository.deleteAllInBatch();
		productTagRepository.deleteAllInBatch();
		guideResponseRepository.deleteAllInBatch();
		journeyStampRepository.deleteAllInBatch();
		passportSessionRepository.deleteAllInBatch();
		passportCardRepository.deleteAllInBatch();
	}
}
