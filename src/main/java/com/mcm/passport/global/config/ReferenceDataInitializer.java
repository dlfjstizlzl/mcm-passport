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
	public static final String THIRD_TEST_PRODUCT_NAME = "Diamant 3D Shoulder Bag";
	public static final String TEST_PRODUCT_DESCRIPTION = "MCM의 시그니처 Visetos 모노그램 캔버스와 나파 가죽 트림을 결합한 아이코닉 백팩입니다. 조절 가능한 숄더 스트랩과 다양한 수납공간, 노트북 또는 태블릿용 내부 수납부를 갖춰 일상과 여행에 실용적으로 활용할 수 있습니다.";
	public static final String SECOND_TEST_PRODUCT_DESCRIPTION = "MCM의 시그니처 Visetos 모노그램 패턴과 실용적인 구조를 결합한 데일리 크로스바디 백입니다. 가볍고 내구성이 뛰어난 코티드 캔버스와 나파 가죽 트림을 사용해 일상생활과 여행에 편안하게 활용할 수 있습니다.";
	public static final String THIRD_TEST_PRODUCT_DESCRIPTION = "MCM의 상징적인 바이에른 다이아몬드를 입체적으로 재해석한 숄더백입니다. 이탈리아산 카프스킨 레더로 제작되었으며, 조형적인 기하학적 호보 실루엣과 양방향 사선 지퍼가 현대적인 분위기를 연출합니다.";

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

		upsertProduct(TEST_PRODUCT_NAME, "BACKPACK", TEST_PRODUCT_DESCRIPTION, "BLACK", "VISETOS", "STRUCTURED",
				"https://images.mcmworldwide.com/i/mcmworldwide/MMKEAVE12BK001_01/stark-black-m?$w1000$&fmt=auto&qlt=default");
		upsertProduct(SECOND_TEST_PRODUCT_NAME, "CROSSBODY", SECOND_TEST_PRODUCT_DESCRIPTION, "COGNAC", "VISETOS", "COMPACT",
				"https://images.mcmworldwide.com/i/mcmworldwide/MMRFSTA05CO001_01?$pdp-large$");
		upsertProduct(THIRD_TEST_PRODUCT_NAME, "SHOULDER_BAG", THIRD_TEST_PRODUCT_DESCRIPTION, "BLACK", "CALF_LEATHER", "GEOMETRIC_HOBO",
				"https://mcmworldwide.sa/cdn/shop/files/Diamant-3D-Shoulder-Bag-in-Visetos-Leather-Mix_cf9af2a3-808e-45ac-9df8-90f7aa4d12db.jpg?v=1753352688");
	}

	private void upsertProduct(
			String name,
			String category,
			String description,
			String color,
			String material,
			String silhouette,
			String imageUrl
	) {
		Product product = productRepository.findFirstByName(name)
				.orElseGet(() -> productRepository.save(Product.create(
						name, category, description, color, material, silhouette, imageUrl, true
				)));
		product.updateCatalogData(category, description, color, material, silhouette, imageUrl, true);
	}
}
