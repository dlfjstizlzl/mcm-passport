package com.mcm.passport.domain.style;

import com.mcm.passport.domain.journey.entity.GuideOption;
import com.mcm.passport.domain.journey.entity.GuideQuestion;
import com.mcm.passport.domain.journey.entity.GuideResponse;
import com.mcm.passport.domain.journey.entity.JourneyStamp;
import com.mcm.passport.domain.journey.entity.JourneySpot;
import com.mcm.passport.domain.journey.repository.GuideOptionRepository;
import com.mcm.passport.domain.journey.repository.GuideQuestionRepository;
import com.mcm.passport.domain.journey.repository.GuideResponseRepository;
import com.mcm.passport.domain.journey.repository.JourneySpotRepository;
import com.mcm.passport.domain.journey.repository.JourneyStampRepository;
import com.mcm.passport.domain.passport.entity.PassportCard;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.repository.PassportCardRepository;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.product.entity.Product;
import com.mcm.passport.domain.product.entity.ProductTag;
import com.mcm.passport.domain.product.repository.ProductRepository;
import com.mcm.passport.domain.product.repository.ProductTagRepository;
import com.mcm.passport.domain.style.entity.RecommendedProduct;

import java.util.List;
import java.util.UUID;

final class IntegratedStyleTestFixture {

	private final PassportCardRepository passportCardRepository;
	private final PassportSessionRepository passportSessionRepository;
	private final JourneySpotRepository journeySpotRepository;
	private final GuideQuestionRepository guideQuestionRepository;
	private final GuideOptionRepository guideOptionRepository;
	private final GuideResponseRepository guideResponseRepository;
	private final JourneyStampRepository journeyStampRepository;
	private final ProductRepository productRepository;
	private final ProductTagRepository productTagRepository;

	IntegratedStyleTestFixture(
			PassportCardRepository passportCardRepository,
			PassportSessionRepository passportSessionRepository,
			JourneySpotRepository journeySpotRepository,
			GuideQuestionRepository guideQuestionRepository,
			GuideOptionRepository guideOptionRepository,
			GuideResponseRepository guideResponseRepository,
			JourneyStampRepository journeyStampRepository,
			ProductRepository productRepository,
			ProductTagRepository productTagRepository
	) {
		this.passportCardRepository = passportCardRepository;
		this.passportSessionRepository = passportSessionRepository;
		this.journeySpotRepository = journeySpotRepository;
		this.guideQuestionRepository = guideQuestionRepository;
		this.guideOptionRepository = guideOptionRepository;
		this.guideResponseRepository = guideResponseRepository;
		this.journeyStampRepository = journeyStampRepository;
		this.productRepository = productRepository;
		this.productTagRepository = productTagRepository;
	}

	PassportSession readyToBoardWithJourney(boolean includeProductTag) {
		PassportSession session = passportSessionRepository.saveAndFlush(
				PassportSession.readyToBoard(newCard())
		);
		GuideQuestion cityQuestion = question("CITY_MOOD_ROOM", "CITY_MOOD_SIGNAL");
		GuideQuestion movementQuestion = question("MOVEMENT_DECK", "WEARING_SCENE");
		guideResponseRepository.saveAllAndFlush(List.of(
				GuideResponse.create(session, cityQuestion, option(cityQuestion, "BERLIN_AFTER_DARK")),
				GuideResponse.create(session, movementQuestion, option(movementQuestion, "WEEKEND_TRIP"))
		));
		journeyStampRepository.saveAllAndFlush(
				journeySpotRepository.findAllByRequiredTrueAndActiveTrueOrderBySequenceAsc().stream()
						.map(spot -> JourneyStamp.create(session, spot))
						.toList()
		);
		if (includeProductTag) {
			Product product = productRepository.findFirstByName(RecommendedProduct.STARK_BACKPACK.getDisplayName())
					.orElseGet(() -> productRepository.save(Product.create(
							RecommendedProduct.STARK_BACKPACK.getDisplayName(),
							"BACKPACK", "BLACK", "VISETOS", "STRUCTURED", null, true
					)));
			productTagRepository.saveAndFlush(ProductTag.create(session, product));
		}
		return session;
	}

	PassportSession readyToBoardWithoutJourney() {
		return passportSessionRepository.saveAndFlush(PassportSession.readyToBoard(newCard()));
	}

	PassportSession exploring() {
		return passportSessionRepository.saveAndFlush(PassportSession.start(newCard()));
	}

	private PassportCard newCard() {
		return passportCardRepository.saveAndFlush(PassportCard.issue("STYLE-" + UUID.randomUUID()));
	}

	private GuideQuestion question(String spotCode, String questionCode) {
		JourneySpot spot = journeySpotRepository.findByCode(spotCode).orElseThrow();
		return guideQuestionRepository.findByJourneySpot_IdAndCode(spot.getId(), questionCode).orElseThrow();
	}

	private GuideOption option(GuideQuestion question, String optionCode) {
		return guideOptionRepository.findByGuideQuestion_IdAndCode(question.getId(), optionCode).orElseThrow();
	}
}
