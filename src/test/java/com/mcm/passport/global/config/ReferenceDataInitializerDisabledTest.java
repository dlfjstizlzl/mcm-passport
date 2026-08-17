package com.mcm.passport.global.config;

import com.mcm.passport.domain.passport.repository.PassportCardRepository;
import com.mcm.passport.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"mcm.reference-data.enabled=false",
		"mcm.style.analysis.provider=mock",
		"spring.datasource.url=jdbc:h2:mem:reference-data-disabled;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class ReferenceDataInitializerDisabledTest {

	@Autowired private ApplicationContext applicationContext;
	@Autowired private PassportCardRepository passportCardRepository;
	@Autowired private ProductRepository productRepository;

	@Test
	void disablesReferenceDataWithoutCreatingCardsOrProducts() {
		assertThat(applicationContext.getBeansOfType(ReferenceDataInitializer.class)).isEmpty();
		assertThat(passportCardRepository.count()).isZero();
		assertThat(productRepository.count()).isZero();
	}
}
