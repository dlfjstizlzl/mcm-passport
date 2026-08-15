package com.mcm.passport.domain.style;

import com.mcm.passport.domain.journey.entity.JourneyResponse;
import com.mcm.passport.domain.journey.entity.JourneyStamp;
import com.mcm.passport.domain.journey.repository.JourneyResponseRepository;
import com.mcm.passport.domain.journey.repository.JourneyStampRepository;
import com.mcm.passport.domain.passport.entity.PassportSession;
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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class StyleApiIntegrationTest {

	private static final String STYLE_SPOT_CODE = "GATE-S1";

	@Autowired
	private WebApplicationContext applicationContext;

	@Autowired
	private PassportSessionRepository passportSessionRepository;

	@Autowired
	private JourneyResponseRepository journeyResponseRepository;

	@Autowired
	private JourneyStampRepository journeyStampRepository;

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
				.andExpect(jsonPath("$.backgroundCode").value("BERLIN_AFTERDARK"))
				.andExpect(jsonPath("$.matchScore").value(92))
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
				.andExpect(jsonPath("$.backgroundAssetKey").value("berlin-afterdark"));

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
		PassportSession passportSession = passportSessionRepository.saveAndFlush(PassportSession.readyToBoard());
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
				JourneyStamp.create(passportSession, "ORIGIN_GATE"),
				JourneyStamp.create(passportSession, "MATERIAL_LOUNGE"),
				JourneyStamp.create(passportSession, "MOVEMENT_DECK"),
				JourneyStamp.create(passportSession, "CITY_MOOD_ROOM"),
				JourneyStamp.create(passportSession, "PRODUCT_TAGGING")
		));
		Product product = productRepository.saveAndFlush(Product.create(
				RecommendedProduct.STARK_BACKPACK.name(),
				RecommendedProduct.STARK_BACKPACK.getDisplayName()
		));
		productTagRepository.saveAndFlush(ProductTag.create(passportSession, product));
		return passportSession;
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
	}
}
