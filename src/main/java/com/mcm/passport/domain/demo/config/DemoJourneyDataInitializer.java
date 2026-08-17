package com.mcm.passport.domain.demo.config;

import com.mcm.passport.domain.demo.entity.DemoJourneySeed;
import com.mcm.passport.domain.demo.repository.DemoJourneySeedRepository;
import com.mcm.passport.domain.journey.entity.JourneyResponse;
import com.mcm.passport.domain.journey.entity.JourneyStamp;
import com.mcm.passport.domain.journey.entity.JourneySpot;
import com.mcm.passport.domain.journey.repository.JourneyResponseRepository;
import com.mcm.passport.domain.journey.repository.JourneyStampRepository;
import com.mcm.passport.domain.journey.repository.JourneySpotRepository;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.entity.PassportSessionStatus;
import com.mcm.passport.domain.passport.entity.PassportCard;
import com.mcm.passport.domain.passport.repository.PassportCardRepository;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.product.entity.Product;
import com.mcm.passport.domain.product.entity.ProductTag;
import com.mcm.passport.domain.product.repository.ProductRepository;
import com.mcm.passport.domain.product.repository.ProductTagRepository;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@ConditionalOnProperty(name = "mcm.demo.seed", havingValue = "true")
@Order(1)
public class DemoJourneyDataInitializer implements ApplicationRunner {

	public static final String WITHOUT_PRODUCT_TAG_SEED_KEY = "BE2_WITHOUT_PRODUCT_TAG";
	public static final String WITH_PRODUCT_TAG_SEED_KEY = "BE2_WITH_STARK_BACKPACK";

	private static final Logger log = LoggerFactory.getLogger(DemoJourneyDataInitializer.class);

	private final DemoJourneySeedRepository demoJourneySeedRepository;
	private final PassportCardRepository passportCardRepository;
	private final PassportSessionRepository passportSessionRepository;
	private final JourneyResponseRepository journeyResponseRepository;
	private final JourneyStampRepository journeyStampRepository;
	private final JourneySpotRepository journeySpotRepository;
	private final ProductRepository productRepository;
	private final ProductTagRepository productTagRepository;

	public DemoJourneyDataInitializer(
			DemoJourneySeedRepository demoJourneySeedRepository,
			PassportCardRepository passportCardRepository,
			PassportSessionRepository passportSessionRepository,
			JourneyResponseRepository journeyResponseRepository,
			JourneyStampRepository journeyStampRepository,
			JourneySpotRepository journeySpotRepository,
			ProductRepository productRepository,
			ProductTagRepository productTagRepository
	) {
		this.demoJourneySeedRepository = demoJourneySeedRepository;
		this.passportCardRepository = passportCardRepository;
		this.passportSessionRepository = passportSessionRepository;
		this.journeyResponseRepository = journeyResponseRepository;
		this.journeyStampRepository = journeyStampRepository;
		this.journeySpotRepository = journeySpotRepository;
		this.productRepository = productRepository;
		this.productTagRepository = productTagRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		PassportSession withoutProductTag = findOrCreateDemoSession(WITHOUT_PRODUCT_TAG_SEED_KEY, false);
		PassportSession withProductTag = findOrCreateDemoSession(WITH_PRODUCT_TAG_SEED_KEY, true);

		log.info(
				"Demo Journey fixture available: withoutProductTagSessionId={}, withProductTagSessionId={}",
				withoutProductTag.getId(),
				withProductTag.getId()
		);
	}

	private PassportSession findOrCreateDemoSession(String seedKey, boolean includeProductTag) {
		return demoJourneySeedRepository.findById(seedKey)
				.map(seed -> reuseOrReplaceDemoSession(seed, includeProductTag))
				.orElseGet(() -> createDemoSeed(seedKey, includeProductTag));
	}

	private PassportSession reuseOrReplaceDemoSession(DemoJourneySeed seed, boolean includeProductTag) {
		PassportSession passportSession = seed.getPassportSession();
		if (passportSession.getStatus() != PassportSessionStatus.COMPLETED) {
			return passportSession;
		}

		PassportSession replacement = createDemoSession(seed.getSeedKey(), includeProductTag);
		seed.replacePassportSession(replacement);
		return replacement;
	}

	private PassportSession createDemoSeed(String seedKey, boolean includeProductTag) {
		PassportSession passportSession = createDemoSession(seedKey, includeProductTag);
		demoJourneySeedRepository.save(DemoJourneySeed.create(seedKey, passportSession));
		return passportSession;
	}

	private PassportSession createDemoSession(String seedKey, boolean includeProductTag) {
		PassportCard passportCard = passportCardRepository.findByCardUid(seedKey)
				.orElseGet(() -> passportCardRepository.save(PassportCard.issue(seedKey)));
		PassportSession passportSession = passportSessionRepository.save(PassportSession.readyToBoard(passportCard));
		journeyResponseRepository.saveAll(demoResponses(passportSession));
		journeyStampRepository.saveAll(demoStamps(passportSession));

		if (includeProductTag) {
			Product product = findOrCreateStarkBackpack();
			productTagRepository.save(ProductTag.create(passportSession, product));
		}

		return passportSession;
	}

	private List<JourneyResponse> demoResponses(PassportSession passportSession) {
		return List.of(
				JourneyResponse.create(
						passportSession,
						"CITY_MOOD_ROOM",
						"TODAY_MOOD",
						"AFTERDARK",
						"Afterdark movement"
				),
				JourneyResponse.create(
						passportSession,
						"MOVEMENT_DECK",
						"PACE",
						"DYNAMIC",
						"Dynamic movement"
				)
		);
	}

	private List<JourneyStamp> demoStamps(PassportSession passportSession) {
		return List.of(
				stamp(passportSession, "ORIGIN_GATE"),
				stamp(passportSession, "MATERIAL_LOUNGE"),
				stamp(passportSession, "MOVEMENT_DECK"),
				stamp(passportSession, "CITY_MOOD_ROOM")
		);
	}

	private JourneyStamp stamp(PassportSession passportSession, String spotCode) {
		JourneySpot journeySpot = journeySpotRepository.findByCode(spotCode).orElseThrow();
		return JourneyStamp.create(passportSession, journeySpot);
	}

	private Product findOrCreateStarkBackpack() {
		String productCode = RecommendedProduct.STARK_BACKPACK.name();
		return productRepository.findByCode(productCode)
				.orElseGet(() -> productRepository.save(Product.create(
						productCode,
						RecommendedProduct.STARK_BACKPACK.getDisplayName()
				)));
	}
}
