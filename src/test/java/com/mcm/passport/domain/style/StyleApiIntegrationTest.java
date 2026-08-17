package com.mcm.passport.domain.style;

import com.mcm.passport.domain.journey.entity.JourneyResponse;
import com.mcm.passport.domain.journey.entity.JourneyStamp;
import com.mcm.passport.domain.journey.entity.JourneySpot;
import com.mcm.passport.domain.journey.repository.JourneyResponseRepository;
import com.mcm.passport.domain.journey.repository.JourneyStampRepository;
import com.mcm.passport.domain.journey.repository.JourneySpotRepository;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.entity.PassportCard;
import com.mcm.passport.domain.passport.repository.PassportCardRepository;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.product.entity.Product;
import com.mcm.passport.domain.product.entity.ProductTag;
import com.mcm.passport.domain.product.repository.ProductRepository;
import com.mcm.passport.domain.product.repository.ProductTagRepository;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import com.mcm.passport.domain.style.entity.StyleSpot;
import com.mcm.passport.domain.style.repository.JourneySouvenirRepository;
import com.mcm.passport.domain.style.repository.StyleResultRepository;
import com.mcm.passport.domain.style.repository.StyleSpotRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "mcm.style.analysis.provider=mock")
class StyleApiIntegrationTest {

	private static final String STYLE_SPOT_CODE = "GATE-S1";

	@Autowired
	private WebApplicationContext applicationContext;

	@Autowired
	private PassportSessionRepository passportSessionRepository;

	@Autowired
	private PassportCardRepository passportCardRepository;

	@Autowired
	private JourneyResponseRepository journeyResponseRepository;

	@Autowired
	private JourneyStampRepository journeyStampRepository;

	@Autowired
	private JourneySpotRepository journeySpotRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductTagRepository productTagRepository;

	@Autowired
	private StyleSpotRepository styleSpotRepository;

	@Autowired
	private StyleResultRepository styleResultRepository;

	@Autowired
	private JourneySouvenirRepository journeySouvenirRepository;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
		cleanDatabase();
		styleSpotRepository.saveAndFlush(StyleSpot.waiting(STYLE_SPOT_CODE));
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
		styleSpotRepository.saveAndFlush(StyleSpot.waiting(STYLE_SPOT_CODE));
	}

	@Test
	void exposesCompleteStyleFlowAndValidationErrors() throws Exception {
		PassportSession passportSession = createJourneyData();

		mockMvc.perform(post("/api/style-spots/{spotCode}/connections", STYLE_SPOT_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"passportSessionId\":" + passportSession.getId() + "}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.spotCode").value(STYLE_SPOT_CODE))
				.andExpect(jsonPath("$.status").value("CONNECTED"))
				.andExpect(jsonPath("$.passportSessionId").value(passportSession.getId()));

		mockMvc.perform(post("/api/style-spots/{spotCode}/analysis", STYLE_SPOT_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.passportSessionId").value(passportSession.getId()))
				.andExpect(jsonPath("$.cityCode").value("BERLIN_AFTERDARK_NOMAD"))
				.andExpect(jsonPath("$.recommendedProductCode").value(RecommendedProduct.STARK_BACKPACK.name()))
				.andExpect(jsonPath("$.backgroundCode").value("BERLIN_AFTER_DARK"))
				.andExpect(jsonPath("$.backgroundName").value("Berlin After Dark"))
				.andExpect(jsonPath("$.matchScore").value(91))
				.andExpect(jsonPath("$.usedFallback").value(false));

		mockMvc.perform(get("/api/style-spots/{spotCode}/result", STYLE_SPOT_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.passportSessionId").value(passportSession.getId()))
				.andExpect(jsonPath("$.cityCodeName").value("Berlin Afterdark Nomad"))
				.andExpect(jsonPath("$.recommendedProductName").value("Stark Backpack"));

		mockMvc.perform(post("/api/passport-sessions/{passportSessionId}/souvenir", passportSession.getId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.passportSessionId").value(passportSession.getId()))
				.andExpect(jsonPath("$.journeyStamps", hasItem("CITY_MOOD_ROOM")))
				.andExpect(jsonPath("$.taggedProductCodes", hasItem(RecommendedProduct.STARK_BACKPACK.name())));

		mockMvc.perform(post("/api/passport-sessions/{passportSessionId}/souvenir", passportSession.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.passportSessionId").value(passportSession.getId()));

		mockMvc.perform(get("/api/passport-sessions/{passportSessionId}/souvenir", passportSession.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cityCode").value("BERLIN_AFTERDARK_NOMAD"))
				.andExpect(jsonPath("$.backgroundAssetKey").value("berlin-after-dark"));

		mockMvc.perform(post("/api/style-spots/{spotCode}/reset", STYLE_SPOT_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.spotCode").value(STYLE_SPOT_CODE))
				.andExpect(jsonPath("$.status").value("RESET"));

		mockMvc.perform(post("/api/style-spots/{spotCode}/connections", STYLE_SPOT_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.path").value("/api/style-spots/GATE-S1/connections"))
				.andExpect(jsonPath("$.errors[0].field").value("passportSessionId"));
	}

	private PassportSession createJourneyData() {
		PassportCard passportCard = passportCardRepository.saveAndFlush(
				PassportCard.issue("TEST-" + UUID.randomUUID())
		);
		PassportSession passportSession = passportSessionRepository.saveAndFlush(
				PassportSession.readyToBoard(passportCard)
		);
		journeyResponseRepository.saveAll(List.of(
				JourneyResponse.create(
						passportSession,
						"CITY_MOOD_ROOM",
						"TODAY_MOOD",
						"AFTERDARK",
						"Afterdark movement"
				),
				JourneyResponse.create(
						passportSession,
						"MOVEMENT_DECK",
						"PACE",
						"DYNAMIC",
						"Dynamic movement"
				)
		));
		journeyStampRepository.saveAll(List.of(
				stamp(passportSession, "ORIGIN_GATE"),
				stamp(passportSession, "MATERIAL_LOUNGE"),
				stamp(passportSession, "MOVEMENT_DECK"),
				stamp(passportSession, "CITY_MOOD_ROOM")
		));
		Product product = productRepository.saveAndFlush(Product.create(
				RecommendedProduct.STARK_BACKPACK.getDisplayName(),
				"BACKPACK",
				"BLACK",
				"VISETOS",
				"STRUCTURED",
				null,
				true
		));
		productTagRepository.saveAndFlush(ProductTag.create(passportSession, product));
		return passportSession;
	}

	private JourneyStamp stamp(PassportSession passportSession, String spotCode) {
		JourneySpot journeySpot = journeySpotRepository.findByCode(spotCode).orElseThrow();
		return JourneyStamp.create(passportSession, journeySpot);
	}

	private void cleanDatabase() {
		journeySouvenirRepository.deleteAll();
		journeySouvenirRepository.flush();
		styleResultRepository.deleteAllInBatch();
		styleSpotRepository.deleteAllInBatch();
		productTagRepository.deleteAllInBatch();
		journeyStampRepository.deleteAllInBatch();
		journeyResponseRepository.deleteAllInBatch();
		productRepository.deleteAllInBatch();
		passportSessionRepository.deleteAllInBatch();
		passportCardRepository.deleteAllInBatch();
	}
}
