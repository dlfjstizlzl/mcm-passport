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
	public static final String TEST_PRODUCT_DESCRIPTION = "MCM의 아이코닉한 Visetos 소재와 구조적인 실루엣이 돋보이는 블랙 백팩입니다.";
	public static final String SECOND_TEST_PRODUCT_DESCRIPTION = "아이코닉한 Visetos 소재와 컴팩트한 실루엣을 담은 코냑 컬러 크로스바디 백입니다.";

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

		Product starkBackpack = productRepository.findFirstByName(TEST_PRODUCT_NAME)
				.orElseGet(() -> productRepository.save(Product.create(
						TEST_PRODUCT_NAME,
						"BACKPACK",
						TEST_PRODUCT_DESCRIPTION,
						"BLACK",
						"VISETOS",
						"STRUCTURED",
						"https://example.com/stark.jpg",
						true
				)));

		if (starkBackpack.getDescription() == null) {
			starkBackpack.updateDescription(TEST_PRODUCT_DESCRIPTION);
		}

		Product arenCrossbody = productRepository.findFirstByName(SECOND_TEST_PRODUCT_NAME)
				.orElseGet(() -> productRepository.save(Product.create(
						SECOND_TEST_PRODUCT_NAME,
						"CROSSBODY",
						SECOND_TEST_PRODUCT_DESCRIPTION,
						"COGNAC",
						"VISETOS",
						"COMPACT",
						"https://example.com/aren-crossbody.jpg",
						true
				)));
		if (arenCrossbody.getDescription() == null) {
			arenCrossbody.updateDescription(SECOND_TEST_PRODUCT_DESCRIPTION);
		}
	}
}
