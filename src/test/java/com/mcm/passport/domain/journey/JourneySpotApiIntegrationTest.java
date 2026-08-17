package com.mcm.passport.domain.journey;

import com.mcm.passport.domain.journey.config.JourneyGuideDataInitializer;
import com.mcm.passport.domain.journey.entity.JourneySpot;
import com.mcm.passport.domain.journey.repository.GuideOptionRepository;
import com.mcm.passport.domain.journey.repository.GuideQuestionRepository;
import com.mcm.passport.domain.journey.repository.JourneySpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"mcm.style.analysis.provider=mock",
		"spring.datasource.url=jdbc:h2:mem:journey-guide-api;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class JourneySpotApiIntegrationTest {

	@Autowired
	private WebApplicationContext applicationContext;

	@Autowired
	private JourneyGuideDataInitializer initializer;

	@Autowired
	private JourneySpotRepository journeySpotRepository;

	@Autowired
	private GuideQuestionRepository guideQuestionRepository;

	@Autowired
	private GuideOptionRepository guideOptionRepository;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
	}

	@Test
	void getsActiveJourneySpotsInJourneyOrder() throws Exception {
		mockMvc.perform(get("/api/journey-spots"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(4))
				.andExpect(jsonPath("$[0].code").value("ORIGIN_GATE"))
				.andExpect(jsonPath("$[1].code").value("MATERIAL_LOUNGE"))
				.andExpect(jsonPath("$[2].code").value("MOVEMENT_DECK"))
				.andExpect(jsonPath("$[3].code").value("CITY_MOOD_ROOM"));
	}

	@Test
	void getsSpotWithOrderedQuestionsAndOptions() throws Exception {
		JourneySpot cityMoodRoom = journeySpotRepository.findByCode("CITY_MOOD_ROOM").orElseThrow();

		mockMvc.perform(get("/api/journey-spots/{journeySpotId}", cityMoodRoom.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("CITY_MOOD_ROOM"))
				.andExpect(jsonPath("$.questions.length()").value(1))
				.andExpect(jsonPath("$.questions[0].code").value("CITY_MOOD_SIGNAL"))
				.andExpect(jsonPath("$.questions[0].options.length()").value(6));
	}

	@Test
	void returnsNotFoundForUnknownJourneySpot() throws Exception {
		mockMvc.perform(get("/api/journey-spots/{journeySpotId}", 999999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("JOURNEY_SPOT_NOT_FOUND"));
	}

	@Test
	void initializationIsIdempotent() throws Exception {
		initializer.run(new DefaultApplicationArguments(new String[0]));

		org.assertj.core.api.Assertions.assertThat(journeySpotRepository.count()).isEqualTo(4);
		org.assertj.core.api.Assertions.assertThat(guideQuestionRepository.count()).isEqualTo(6);
		org.assertj.core.api.Assertions.assertThat(guideOptionRepository.count()).isEqualTo(27);
	}
}
