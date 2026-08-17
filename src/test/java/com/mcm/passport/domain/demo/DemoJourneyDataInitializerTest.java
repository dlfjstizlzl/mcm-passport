package com.mcm.passport.domain.demo;

import com.mcm.passport.domain.demo.config.DemoJourneyDataInitializer;
import com.mcm.passport.domain.demo.entity.DemoJourneySeed;
import com.mcm.passport.domain.demo.repository.DemoJourneySeedRepository;
import com.mcm.passport.domain.journey.repository.JourneyResponseRepository;
import com.mcm.passport.domain.journey.repository.JourneyStampRepository;
import com.mcm.passport.domain.journey.service.JourneyDataReader;
import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.passport.entity.PassportSessionStatus;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.product.repository.ProductRepository;
import com.mcm.passport.domain.product.repository.ProductTagRepository;
import com.mcm.passport.domain.style.analysis.MockStyleAnalysisProvider;
import com.mcm.passport.domain.style.analysis.OpenAIStyleAnalysisProvider;
import com.mcm.passport.domain.style.analysis.StyleAnalysisProvider;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStyleAnalysisGateway;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import com.openai.client.OpenAIClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"mcm.demo.seed=true",
		"mcm.style.analysis.provider=mock",
		"spring.datasource.url=jdbc:h2:mem:demo-seed-enabled;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoJourneyDataInitializerTest {

	private static final List<String> EXPECTED_STAMPS = List.of(
			"ORIGIN_GATE",
			"MATERIAL_LOUNGE",
			"MOVEMENT_DECK",
			"CITY_MOOD_ROOM"
	);

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private DemoJourneyDataInitializer initializer;

	@Autowired
	private DemoJourneySeedRepository demoJourneySeedRepository;

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
	private JourneyDataReader journeyDataReader;

	@Test
	@Transactional(readOnly = true)
	void seedEnabledCreatesTwoReadyToBoardJourneysWithOptionalProductTagVariants() {
		DemoJourneySeed withoutProductTag = seed(DemoJourneyDataInitializer.WITHOUT_PRODUCT_TAG_SEED_KEY);
		DemoJourneySeed withProductTag = seed(DemoJourneyDataInitializer.WITH_PRODUCT_TAG_SEED_KEY);

		assertThat(demoJourneySeedRepository.count()).isEqualTo(2);
		assertThat(passportSessionRepository.findAll())
				.hasSize(2)
				.allSatisfy(session ->
						assertThat(session.getStatus()).isEqualTo(PassportSessionStatus.READY_TO_BOARD));

		JourneyDataSnapshot withoutProduct = journeyDataReader.read(withoutProductTag.getPassportSession().getId());
		JourneyDataSnapshot withProduct = journeyDataReader.read(withProductTag.getPassportSession().getId());

		assertCommonJourneyFixture(withoutProduct);
		assertThat(withoutProduct.taggedProducts()).isEmpty();

		assertCommonJourneyFixture(withProduct);
		assertThat(withProduct.taggedProducts())
				.singleElement()
				.satisfies(product -> assertThat(product.code())
						.isEqualTo(RecommendedProduct.STARK_BACKPACK.name()));
	}

	@Test
	@Transactional
	void repeatedInitializationReusesDurableSeedMarkersWithoutDuplicates() {
		Map<String, Long> sessionIdsBefore = Map.of(
				DemoJourneyDataInitializer.WITHOUT_PRODUCT_TAG_SEED_KEY,
				seed(DemoJourneyDataInitializer.WITHOUT_PRODUCT_TAG_SEED_KEY).getPassportSession().getId(),
				DemoJourneyDataInitializer.WITH_PRODUCT_TAG_SEED_KEY,
				seed(DemoJourneyDataInitializer.WITH_PRODUCT_TAG_SEED_KEY).getPassportSession().getId()
		);

		initializer.run(new DefaultApplicationArguments(new String[0]));

		assertThat(demoJourneySeedRepository.count()).isEqualTo(2);
		assertThat(passportSessionRepository.count()).isEqualTo(2);
		assertThat(journeyResponseRepository.count()).isEqualTo(4);
		assertThat(journeyStampRepository.count()).isEqualTo(8);
		assertThat(productRepository.count()).isEqualTo(1);
		assertThat(productTagRepository.count()).isEqualTo(1);
		assertThat(seed(DemoJourneyDataInitializer.WITHOUT_PRODUCT_TAG_SEED_KEY).getPassportSession().getId())
				.isEqualTo(sessionIdsBefore.get(DemoJourneyDataInitializer.WITHOUT_PRODUCT_TAG_SEED_KEY));
		assertThat(seed(DemoJourneyDataInitializer.WITH_PRODUCT_TAG_SEED_KEY).getPassportSession().getId())
				.isEqualTo(sessionIdsBefore.get(DemoJourneyDataInitializer.WITH_PRODUCT_TAG_SEED_KEY));
	}

	@Test
	@Transactional
	void inProgressSeedSessionIsPreservedUntilExplicitStyleSpotRecovery() {
		DemoJourneySeed marker = seed(DemoJourneyDataInitializer.WITHOUT_PRODUCT_TAG_SEED_KEY);
		Long inProgressSessionId = marker.getPassportSession().getId();
		marker.getPassportSession().enterStyleSpot();
		passportSessionRepository.flush();

		initializer.run(new DefaultApplicationArguments(new String[0]));

		DemoJourneySeed preservedMarker = seed(DemoJourneyDataInitializer.WITHOUT_PRODUCT_TAG_SEED_KEY);
		assertThat(preservedMarker.getPassportSession().getId()).isEqualTo(inProgressSessionId);
		assertThat(preservedMarker.getPassportSession().getStatus()).isEqualTo(PassportSessionStatus.STYLE_SPOT);
		assertOriginalSeedCounts();
	}

	@Test
	@Transactional
	void completedSeedSessionIsReplacedOnceAndTheReadyReplacementIsThenReused() {
		DemoJourneySeed marker = seed(DemoJourneyDataInitializer.WITHOUT_PRODUCT_TAG_SEED_KEY);
		Long consumedSessionId = marker.getPassportSession().getId();
		marker.getPassportSession().enterStyleSpot();
		marker.getPassportSession().complete();
		passportSessionRepository.flush();

		initializer.run(new DefaultApplicationArguments(new String[0]));

		DemoJourneySeed replacedMarker = seed(DemoJourneyDataInitializer.WITHOUT_PRODUCT_TAG_SEED_KEY);
		Long replacementSessionId = replacedMarker.getPassportSession().getId();
		assertThat(replacementSessionId).isNotEqualTo(consumedSessionId);
		assertThat(replacedMarker.getPassportSession().getStatus())
				.isEqualTo(PassportSessionStatus.READY_TO_BOARD);
		assertThat(journeyDataReader.read(replacementSessionId).taggedProducts()).isEmpty();
		assertCurrentReplacementCounts();

		initializer.run(new DefaultApplicationArguments(new String[0]));

		assertThat(seed(DemoJourneyDataInitializer.WITHOUT_PRODUCT_TAG_SEED_KEY)
				.getPassportSession().getId()).isEqualTo(replacementSessionId);
		assertCurrentReplacementCounts();
	}

	@Test
	void seedEnabledWithMockProviderCreatesNoOpenAIClientOrGateway() {
		Map<String, StyleAnalysisProvider> providers = applicationContext
				.getBeansOfType(StyleAnalysisProvider.class);

		assertThat(demoJourneySeedRepository.count()).isEqualTo(2);
		assertThat(providers).hasSize(1);
		assertThat(providers.values())
				.singleElement()
				.isInstanceOf(MockStyleAnalysisProvider.class);
		assertThat(applicationContext.getBeansOfType(OpenAIStyleAnalysisProvider.class)).isEmpty();
		assertThat(applicationContext.getBeansOfType(OpenAIStyleAnalysisGateway.class)).isEmpty();
		assertThat(applicationContext.getBeansOfType(OpenAIClient.class)).isEmpty();
	}

	@AfterAll
	void cleanSeedDatabaseInForeignKeyOrder() {
		demoJourneySeedRepository.deleteAllInBatch();
		productTagRepository.deleteAllInBatch();
		journeyStampRepository.deleteAllInBatch();
		journeyResponseRepository.deleteAllInBatch();
		productRepository.deleteAllInBatch();
		passportSessionRepository.deleteAllInBatch();
	}

	private DemoJourneySeed seed(String seedKey) {
		return demoJourneySeedRepository.findById(seedKey).orElseThrow();
	}

	private void assertCommonJourneyFixture(JourneyDataSnapshot journeyData) {
		assertThat(journeyData.responses())
				.extracting(JourneyDataSnapshot.ResponseSignal::answerCode)
				.containsExactly("AFTERDARK", "DYNAMIC");
		assertThat(journeyData.stamps())
				.extracting(JourneyDataSnapshot.StampSignal::spotCode)
				.containsExactlyElementsOf(EXPECTED_STAMPS);
	}

	private void assertCurrentReplacementCounts() {
		assertThat(demoJourneySeedRepository.count()).isEqualTo(2);
		assertThat(passportSessionRepository.count()).isEqualTo(3);
		assertThat(journeyResponseRepository.count()).isEqualTo(6);
		assertThat(journeyStampRepository.count()).isEqualTo(12);
		assertThat(productRepository.count()).isEqualTo(1);
		assertThat(productTagRepository.count()).isEqualTo(1);
	}

	private void assertOriginalSeedCounts() {
		assertThat(demoJourneySeedRepository.count()).isEqualTo(2);
		assertThat(passportSessionRepository.count()).isEqualTo(2);
		assertThat(journeyResponseRepository.count()).isEqualTo(4);
		assertThat(journeyStampRepository.count()).isEqualTo(8);
		assertThat(productRepository.count()).isEqualTo(1);
		assertThat(productTagRepository.count()).isEqualTo(1);
	}
}
