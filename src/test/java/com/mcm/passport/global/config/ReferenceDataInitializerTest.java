package com.mcm.passport.global.config;

import com.mcm.passport.domain.passport.repository.PassportCardRepository;
import com.mcm.passport.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"mcm.style.analysis.provider=mock",
		"spring.datasource.url=jdbc:h2:mem:reference-data-initializer;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class ReferenceDataInitializerTest {

	@Autowired
	private ReferenceDataInitializer initializer;

	@Autowired
	private PassportCardRepository passportCardRepository;

	@Autowired
	private ProductRepository productRepository;

	@Test
	void createsReferenceDataOnce() throws Exception {
		initializer.run(new DefaultApplicationArguments(new String[0]));

		var card = passportCardRepository.findByCardUid(ReferenceDataInitializer.TEST_CARD_UID).orElseThrow();
		var starkBackpack = productRepository.findFirstByName(ReferenceDataInitializer.TEST_PRODUCT_NAME).orElseThrow();
		var arenCrossbody = productRepository.findFirstByName(ReferenceDataInitializer.SECOND_TEST_PRODUCT_NAME).orElseThrow();
		var diamantShoulderBag = productRepository.findFirstByName(ReferenceDataInitializer.THIRD_TEST_PRODUCT_NAME).orElseThrow();

		assertThat(card.isActive()).isTrue();
		assertThat(starkBackpack.getCategory()).isEqualTo("BACKPACK");
		assertThat(starkBackpack.getDescription()).isEqualTo(ReferenceDataInitializer.TEST_PRODUCT_DESCRIPTION);
		assertThat(starkBackpack.getColor()).isEqualTo("BLACK");
		assertThat(starkBackpack.getMaterial()).isEqualTo("VISETOS");
		assertThat(starkBackpack.getSilhouette()).isEqualTo("STRUCTURED");
		assertThat(starkBackpack.getImageUrl()).isEqualTo(
				"https://images.mcmworldwide.com/i/mcmworldwide/MMKEAVE12BK001_01/stark-black-m?$w1000$&fmt=auto&qlt=default"
		);
		assertThat(starkBackpack.isRecommendable()).isTrue();
		assertThat(starkBackpack.isActive()).isTrue();
		assertThat(arenCrossbody.getDescription()).isEqualTo(ReferenceDataInitializer.SECOND_TEST_PRODUCT_DESCRIPTION);
		assertThat(arenCrossbody.getImageUrl()).isEqualTo(
				"https://images.mcmworldwide.com/i/mcmworldwide/MMRFSTA05CO001_01?$pdp-large$"
		);
		assertThat(diamantShoulderBag.getCategory()).isEqualTo("SHOULDER_BAG");
		assertThat(diamantShoulderBag.getDescription()).isEqualTo(ReferenceDataInitializer.THIRD_TEST_PRODUCT_DESCRIPTION);
		assertThat(diamantShoulderBag.getMaterial()).isEqualTo("CALF_LEATHER");
		assertThat(diamantShoulderBag.getSilhouette()).isEqualTo("GEOMETRIC_HOBO");
		assertThat(passportCardRepository.findAll().stream()
				.filter(candidate -> candidate.getCardUid().equals(ReferenceDataInitializer.TEST_CARD_UID)))
				.hasSize(1);
		assertThat(productRepository.findAllByActiveTrueOrderByIdAsc())
				.extracting(product -> product.getName())
				.containsExactlyInAnyOrder(
						ReferenceDataInitializer.TEST_PRODUCT_NAME,
						ReferenceDataInitializer.SECOND_TEST_PRODUCT_NAME,
						ReferenceDataInitializer.THIRD_TEST_PRODUCT_NAME
				);
	}
}
