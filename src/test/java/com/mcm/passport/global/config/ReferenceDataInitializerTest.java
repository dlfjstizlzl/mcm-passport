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
		var product = productRepository.findFirstByName(ReferenceDataInitializer.TEST_PRODUCT_NAME).orElseThrow();

		assertThat(card.isActive()).isTrue();
		assertThat(product.getCategory()).isEqualTo("BACKPACK");
		assertThat(product.getColor()).isEqualTo("BLACK");
		assertThat(product.getMaterial()).isEqualTo("VISETOS");
		assertThat(product.getSilhouette()).isEqualTo("STRUCTURED");
		assertThat(product.getImageUrl()).isEqualTo("https://example.com/stark.jpg");
		assertThat(product.isRecommendable()).isTrue();
		assertThat(product.isActive()).isTrue();
		assertThat(passportCardRepository.findAll().stream()
				.filter(candidate -> candidate.getCardUid().equals(ReferenceDataInitializer.TEST_CARD_UID)))
				.hasSize(1);
		assertThat(productRepository.findAll().stream()
				.filter(candidate -> candidate.getName().equals(ReferenceDataInitializer.TEST_PRODUCT_NAME)))
				.hasSize(1);
	}
}
