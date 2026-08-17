package com.mcm.passport.domain.style;

import com.mcm.passport.domain.passport.entity.PassportCard;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.repository.PassportCardRepository;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.style.analysis.ValidatedStyleAnalysis;
import com.mcm.passport.domain.style.entity.CityBackground;
import com.mcm.passport.domain.style.entity.CityCode;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import com.mcm.passport.domain.style.entity.StyleMood;
import com.mcm.passport.domain.style.entity.StyleResult;
import com.mcm.passport.domain.style.repository.StylePortraitRepository;
import com.mcm.passport.domain.style.repository.StyleResultRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"mcm.style.analysis.provider=mock",
		"mcm.reference-data.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:style-portrait-contract;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class StylePortraitApiContractTest {

	@Autowired private WebApplicationContext applicationContext;
	@Autowired private PassportSessionRepository passportSessionRepository;
	@Autowired private PassportCardRepository passportCardRepository;
	@Autowired private StyleResultRepository styleResultRepository;
	@Autowired private StylePortraitRepository stylePortraitRepository;

	private MockMvc mockMvc;
	private PassportSession session;
	private ExecutorService executor;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
		stylePortraitRepository.deleteAllInBatch();
		styleResultRepository.deleteAllInBatch();
		passportSessionRepository.deleteAllInBatch();
		passportCardRepository.deleteAllInBatch();

		PassportCard card = passportCardRepository.saveAndFlush(
				PassportCard.issue("PORTRAIT-" + UUID.randomUUID())
		);
		session = passportSessionRepository.saveAndFlush(PassportSession.readyToBoard(card));
		styleResultRepository.saveAndFlush(StyleResult.create(session, validAnalysis(), false));
	}

	@AfterEach
	void shutDownExecutor() throws InterruptedException {
		if (executor != null) {
			executor.shutdownNow();
			executor.awaitTermination(5, TimeUnit.SECONDS);
		}
	}

	@Test
	void requiresExplicitTrueConsent() throws Exception {
		assertInvalidPortrait("{\"imageUrl\":\"image-key\"}", "consent");
		assertInvalidPortrait("{\"imageUrl\":\"image-key\",\"consent\":false}", "consent");

		mockMvc.perform(post(portraitPath(), session.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"imageUrl\":\"image-key\",\"consent\":true}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.consent").value(true));
	}

	@Test
	void validatesBlankAndMaximumImageUrlLength() throws Exception {
		assertInvalidPortrait("{\"imageUrl\":\"   \",\"consent\":true}", "imageUrl");

		String maximumLengthImageUrl = "a".repeat(1000);
		mockMvc.perform(post(portraitPath(), session.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(portraitBody(maximumLengthImageUrl)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.imageUrl").value(maximumLengthImageUrl));

		stylePortraitRepository.deleteAllInBatch();
		assertInvalidPortrait(portraitBody("a".repeat(1001)), "imageUrl");
	}

	@Test
	void returnsConflictForSequentialDuplicateCreation() throws Exception {
		mockMvc.perform(post(portraitPath(), session.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(portraitBody("first-image")))
				.andExpect(status().isCreated());

		mockMvc.perform(post(portraitPath(), session.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(portraitBody("second-image")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
	}

	@Test
	void serializesConcurrentCreationAsCreatedAndConflict() throws Exception {
		executor = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);

		Future<MvcResult> first = executor.submit(() -> createPortraitWhenReleased(start, "first-image"));
		Future<MvcResult> second = executor.submit(() -> createPortraitWhenReleased(start, "second-image"));
		start.countDown();

		List<MvcResult> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
		assertThat(results).extracting(result -> result.getResponse().getStatus())
				.containsExactlyInAnyOrder(201, 409);
		assertThat(results.stream()
				.filter(result -> result.getResponse().getStatus() == 409)
				.findFirst()
				.orElseThrow()
				.getResponse()
				.getContentAsString())
				.contains("\"code\":\"DUPLICATE_RESOURCE\"");
		assertThat(stylePortraitRepository.count()).isEqualTo(1);
	}

	@Test
	void deleteReturnsNoContentThenNotFound() throws Exception {
		mockMvc.perform(post(portraitPath(), session.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(portraitBody("image-key")))
				.andExpect(status().isCreated());

		mockMvc.perform(delete(portraitPath(), session.getId()))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete(portraitPath(), session.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("STYLE_PORTRAIT_NOT_FOUND"));
	}

	private void assertInvalidPortrait(String content, String field) throws Exception {
		mockMvc.perform(post(portraitPath(), session.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(content))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.errors[?(@.field == '" + field + "')]").exists());
	}

	private MvcResult createPortraitWhenReleased(CountDownLatch start, String imageUrl) throws Exception {
		if (!start.await(5, TimeUnit.SECONDS)) {
			throw new IllegalStateException("Concurrent portrait requests did not start in time");
		}
		return mockMvc.perform(post(portraitPath(), session.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(portraitBody(imageUrl)))
				.andReturn();
	}

	private String portraitBody(String imageUrl) {
		return "{\"imageUrl\":\"" + imageUrl + "\",\"consent\":true}";
	}

	private String portraitPath() {
		return "/api/passport-sessions/{passportSessionId}/portrait";
	}

	private ValidatedStyleAnalysis validAnalysis() {
		return new ValidatedStyleAnalysis(
				CityCode.BERLIN_AFTERDARK_NOMAD,
				RecommendedProduct.STARK_BACKPACK,
				StyleMood.AFTERDARK_MOVEMENT,
				CityBackground.BERLIN_AFTER_DARK,
				"Portrait contract test result.",
				90
		);
	}
}
