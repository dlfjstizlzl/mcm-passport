package com.mcm.passport.global.config;

import com.mcm.passport.domain.passport.entity.PassportCard;
import com.mcm.passport.domain.passport.repository.PassportCardRepository;
import com.mcm.passport.domain.product.entity.Product;
import com.mcm.passport.domain.product.repository.ProductRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
		name = "mcm.reference-data.enabled",
		havingValue = "true",
		matchIfMissing = true
)
@Order(0)
public class ReferenceDataInitializer implements ApplicationRunner {

	public static final String TEST_CARD_UID = "MCM-GUIDE-TEST-001";
	public static final String TEST_PRODUCT_NAME = "Stark Backpack";
	public static final String SECOND_TEST_PRODUCT_NAME = "Aren Crossbody";

	private final PassportCardRepository passportCardRepository;
	private final ProductRepository productRepository;

	public ReferenceDataInitializer(
			PassportCardRepository passportCardRepository,
			ProductRepository productRepository
	) {
		this.passportCardRepository = passportCardRepository;
		this.productRepository = productRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		passportCardRepository.findByCardUid(TEST_CARD_UID)
				.orElseGet(() -> passportCardRepository.save(PassportCard.issue(TEST_CARD_UID)));

		productRepository.findFirstByName(TEST_PRODUCT_NAME)
				.orElseGet(() -> productRepository.save(Product.create(
						TEST_PRODUCT_NAME,
						"BACKPACK",
						"BLACK",
						"VISETOS",
						"STRUCTURED",
						"https://example.com/stark.jpg",
						true
				)));

		productRepository.findFirstByName(SECOND_TEST_PRODUCT_NAME)
				.orElseGet(() -> productRepository.save(Product.create(
						SECOND_TEST_PRODUCT_NAME,
						"CROSSBODY",
						"COGNAC",
						"VISETOS",
						"COMPACT",
						"https://example.com/aren-crossbody.jpg",
						true
				)));
	}
}
