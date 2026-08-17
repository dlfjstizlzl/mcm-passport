package com.mcm.passport.domain.demo;

import com.mcm.passport.domain.demo.config.DemoJourneyDataInitializer;
import com.mcm.passport.domain.demo.repository.DemoJourneySeedRepository;
import com.mcm.passport.domain.journey.repository.JourneyResponseRepository;
import com.mcm.passport.domain.journey.repository.JourneyStampRepository;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.product.repository.ProductRepository;
import com.mcm.passport.domain.product.repository.ProductTagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"mcm.demo.seed=false",
		"mcm.style.analysis.provider=mock",
		"spring.datasource.url=jdbc:h2:mem:demo-seed-disabled;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class DemoJourneyDataInitializerDisabledTest {

	@Autowired
	private ApplicationContext applicationContext;

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

	@Test
	void seedDisabledByDefaultContractCreatesNoDemoJourneyFixture() {
		assertThat(applicationContext.getBeansOfType(DemoJourneyDataInitializer.class)).isEmpty();
		assertThat(demoJourneySeedRepository.count()).isZero();
		assertThat(passportSessionRepository.count()).isZero();
		assertThat(journeyResponseRepository.count()).isZero();
		assertThat(journeyStampRepository.count()).isZero();
		assertThat(productTagRepository.count()).isZero();
		assertThat(productRepository.count()).isEqualTo(1);
	}

	@Test
	void productionConfigurationKeepsDemoSeedEnvironmentFallbackDisabled() throws IOException {
		YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
		Object configuredValue = loader.load(
				"main-application",
				new FileSystemResource("src/main/resources/application.yaml")
		).stream()
				.map(propertySource -> propertySource.getProperty("mcm.demo.seed"))
				.filter(value -> value != null)
				.findFirst()
				.orElseThrow();

		assertThat(configuredValue).isEqualTo("${MCM_DEMO_SEED:false}");
	}
}
