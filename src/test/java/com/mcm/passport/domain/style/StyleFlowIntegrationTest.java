package com.mcm.passport.domain.style;

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
import com.mcm.passport.domain.style.analysis.StyleAnalysisDecision;
import com.mcm.passport.domain.style.analysis.ValidatedStyleAnalysis;
import com.mcm.passport.domain.style.dto.JourneySouvenirResponse;
import com.mcm.passport.domain.style.dto.StyleResultResponse;
import com.mcm.passport.domain.style.dto.StyleSpotResponse;
import com.mcm.passport.domain.style.entity.CityBackground;
import com.mcm.passport.domain.style.entity.CityCode;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import com.mcm.passport.domain.style.entity.StyleMood;
import com.mcm.passport.domain.style.entity.StyleSpot;
import com.mcm.passport.domain.style.entity.StyleSpotStatus;
import com.mcm.passport.domain.style.repository.JourneySouvenirRepository;
import com.mcm.passport.domain.style.repository.StyleResultRepository;
import com.mcm.passport.domain.style.repository.StyleSpotRepository;
import com.mcm.passport.domain.style.service.JourneySouvenirService;
import com.mcm.passport.domain.style.service.JourneySouvenirCreation;
import com.mcm.passport.domain.style.service.StyleAnalysisService;
import com.mcm.passport.domain.style.service.StyleAnalysisPreparation;
import com.mcm.passport.domain.style.service.StyleAnalysisTransactionService;
import com.mcm.passport.domain.style.service.StyleSpotService;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "mcm.style.analysis.provider=mock")
class StyleFlowIntegrationTest {

	private static final String STYLE_SPOT_CODE = "GATE-S1";

	@Autowired
	private StyleSpotService styleSpotService;

	@Autowired
	private StyleAnalysisService styleAnalysisService;

	@Autowired
	private StyleAnalysisTransactionService styleAnalysisTransactionService;

	@Autowired
	private JourneySouvenirService journeySouvenirService;

	@Autowired
	private PassportSessionRepository passportSessionRepository;

	@Autowired
	private PassportCardRepository passportCardRepository;

	@Autowired
	private JourneyResponseRepository journeyResponseRepository;

	@Autowired
	private JourneyStampRepository journeyStampRepository;

	@Autowired
	private JourneySpotRepository journeySpotRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductTagRepository productTagRepository;

	@Autowired
	private StyleSpotRepository styleSpotRepository;

	@Autowired
	private StyleResultRepository styleResultRepository;

	@Autowired
	private JourneySouvenirRepository journeySouvenirRepository;

	@BeforeEach
	void setUp() {
		cleanDatabase();
		styleSpotRepository.saveAndFlush(StyleSpot.waiting(STYLE_SPOT_CODE));
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
		styleSpotRepository.saveAndFlush(StyleSpot.waiting(STYLE_SPOT_CODE));
	}

	@Test
	void completesCurrentPrototypeStyleAndResultFlowFromReadyToBoardSession() {
		PassportSession passportSession = createJourneyData();

		StyleSpotResponse connected = styleSpotService.connect(STYLE_SPOT_CODE, passportSession.getId());

		assertThat(connected.status()).isEqualTo(StyleSpotStatus.CONNECTED);
		assertThat(connected.passportSessionId()).isEqualTo(passportSession.getId());
		assertThat(reloadSession(passportSession).getStatus()).isEqualTo(PassportSessionStatus.STYLE_SPOT);

		StyleResultResponse analyzed = styleAnalysisService.analyze(STYLE_SPOT_CODE);

		assertThat(analyzed.cityCode()).isEqualTo(CityCode.BERLIN_AFTERDARK_NOMAD.name());
		assertThat(analyzed.recommendedProductCode()).isEqualTo(RecommendedProduct.STARK_BACKPACK.name());
		assertThat(analyzed.styleMood()).isEqualTo(StyleMood.AFTERDARK_MOVEMENT.name());
		assertThat(analyzed.backgroundCode()).isEqualTo(CityBackground.BERLIN_AFTER_DARK.name());
		assertThat(analyzed.matchScore()).isEqualTo(91);
		assertThat(analyzed.usedFallback()).isFalse();
		assertThat(styleSpotRepository.findById(STYLE_SPOT_CODE).orElseThrow().getStatus())
				.isEqualTo(StyleSpotStatus.RESULT);
		assertThat(reloadSession(passportSession).getStatus()).isEqualTo(PassportSessionStatus.STYLE_SPOT);

		StyleResultResponse fetchedResult = styleAnalysisService.getResult(STYLE_SPOT_CODE);
		StyleResultResponse duplicateAnalysis = styleAnalysisService.analyze(STYLE_SPOT_CODE);

		assertThat(fetchedResult.id()).isEqualTo(analyzed.id());
		assertThat(fetchedResult.passportSessionId()).isEqualTo(passportSession.getId());
		assertThat(duplicateAnalysis.id()).isEqualTo(analyzed.id());
		assertThat(styleResultRepository.count()).isEqualTo(1);

		JourneySouvenirCreation created = journeySouvenirService.create(passportSession.getId());

		assertThat(created.created()).isTrue();
		assertThat(reloadSession(passportSession).getStatus()).isEqualTo(PassportSessionStatus.COMPLETED);

		JourneySouvenirCreation duplicate = journeySouvenirService.create(passportSession.getId());
		JourneySouvenirResponse createdSouvenir = created.souvenir();
		JourneySouvenirResponse duplicateCreate = duplicate.souvenir();
		JourneySouvenirResponse fetchedSouvenir = journeySouvenirService.get(passportSession.getId());

		assertThat(duplicate.created()).isFalse();
		assertThat(duplicateCreate.id()).isEqualTo(createdSouvenir.id());
		assertThat(fetchedSouvenir.id()).isEqualTo(createdSouvenir.id());
		assertThat(journeySouvenirRepository.count()).isEqualTo(1);
		assertThat(fetchedSouvenir.journeyStamps()).contains("CITY_MOOD_ROOM");
		assertThat(fetchedSouvenir.taggedProductCodes()).contains(RecommendedProduct.STARK_BACKPACK.name());
		assertThat(reloadSession(passportSession).getStatus()).isEqualTo(PassportSessionStatus.COMPLETED);
		assertThat(reloadSession(passportSession).getCompletedAt()).isNotNull();

		StyleSpotResponse reset = styleSpotService.reset(STYLE_SPOT_CODE);

		assertThat(reset.status()).isEqualTo(StyleSpotStatus.RESET);
		assertThat(reset.passportSessionId()).isNull();
		assertThat(styleSpotRepository.findById(STYLE_SPOT_CODE).orElseThrow().getStatus())
				.isEqualTo(StyleSpotStatus.RESET);
	}

	@Test
	void prototypeFlowAnalyzesReadySessionWithoutOptionalProductTag() {
		PassportSession passportSession = createJourneyDataWithoutProductTag();

		assertThat(reloadSession(passportSession).getStatus()).isEqualTo(PassportSessionStatus.READY_TO_BOARD);
		assertThat(productTagRepository.count()).isZero();

		StyleSpotResponse connected = styleSpotService.connect(STYLE_SPOT_CODE, passportSession.getId());
		StyleResultResponse analyzed = styleAnalysisService.analyze(STYLE_SPOT_CODE);

		assertThat(connected.status()).isEqualTo(StyleSpotStatus.CONNECTED);
		assertThat(analyzed.id()).isNotNull();
		assertThat(analyzed.passportSessionId()).isEqualTo(passportSession.getId());
		assertThat(styleResultRepository.count()).isEqualTo(1);
		assertThat(styleSpotRepository.findById(STYLE_SPOT_CODE).orElseThrow().getStatus())
				.isEqualTo(StyleSpotStatus.RESULT);
		assertThat(reloadSession(passportSession).getStatus()).isEqualTo(PassportSessionStatus.STYLE_SPOT);
	}

	@Test
	void rejectsStyleSpotConnectionBeforeBoardingIsReady() {
		PassportSession activeSession = saveExploringSession();

		assertThatThrownBy(() -> styleSpotService.connect(STYLE_SPOT_CODE, activeSession.getId()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_SESSION_STATUS));

		assertThat(styleSpotRepository.findById(STYLE_SPOT_CODE).orElseThrow().getStatus())
				.isEqualTo(StyleSpotStatus.WAITING);
	}

	@Test
	void rejectsAnalysisWhenReadySessionHasNoJourneyData() {
		PassportSession passportSession = saveReadyToBoardSession();
		styleSpotService.connect(STYLE_SPOT_CODE, passportSession.getId());

		assertThatThrownBy(() -> styleAnalysisService.analyze(STYLE_SPOT_CODE))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.JOURNEY_NOT_COMPLETED));

		assertThat(styleSpotRepository.findById(STYLE_SPOT_CODE).orElseThrow().getStatus())
				.isEqualTo(StyleSpotStatus.CONNECTED);
		assertThat(styleResultRepository.count()).isZero();
	}

	@Test
	void resetRecoversAnAnalyzingSpotAndAllowsReconnect() {
		PassportSession passportSession = createJourneyData();
		styleSpotService.connect(STYLE_SPOT_CODE, passportSession.getId());
		StyleAnalysisPreparation preparation = styleAnalysisTransactionService.prepare(STYLE_SPOT_CODE);

		assertThat(preparation.alreadyCompleted()).isFalse();
		assertThat(styleSpotRepository.findById(STYLE_SPOT_CODE).orElseThrow().getStatus())
				.isEqualTo(StyleSpotStatus.ANALYZING);

		StyleSpotResponse reset = styleSpotService.reset(STYLE_SPOT_CODE);

		assertThat(reset.status()).isEqualTo(StyleSpotStatus.RESET);
		assertThat(reloadSession(passportSession).getStatus()).isEqualTo(PassportSessionStatus.READY_TO_BOARD);

		StyleSpotResponse reconnected = styleSpotService.connect(STYLE_SPOT_CODE, passportSession.getId());

		assertThat(reconnected.status()).isEqualTo(StyleSpotStatus.CONNECTED);
	}

	@Test
	void stalePrototypeAnalysisAttemptCannotOverwriteTheCurrentResult() {
		PassportSession passportSession = createJourneyData();
		styleSpotService.connect(STYLE_SPOT_CODE, passportSession.getId());
		StyleAnalysisPreparation stalePreparation = styleAnalysisTransactionService.prepare(STYLE_SPOT_CODE);

		styleSpotService.reset(STYLE_SPOT_CODE);
		styleSpotService.connect(STYLE_SPOT_CODE, passportSession.getId());
		StyleAnalysisPreparation currentPreparation = styleAnalysisTransactionService.prepare(STYLE_SPOT_CODE);
		StyleAnalysisDecision decision = validAnalysisDecision();

		assertThat(currentPreparation.analysisAttempt()).isGreaterThan(stalePreparation.analysisAttempt());
		assertThatThrownBy(() -> styleAnalysisTransactionService.complete(stalePreparation, decision))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_SESSION_STATUS));
		assertThat(styleResultRepository.count()).isZero();
		assertThat(styleSpotRepository.findById(STYLE_SPOT_CODE).orElseThrow().getStatus())
				.isEqualTo(StyleSpotStatus.ANALYZING);

		styleAnalysisTransactionService.fail(stalePreparation);

		StyleSpot analyzingSpot = styleSpotRepository.findById(STYLE_SPOT_CODE).orElseThrow();
		assertThat(analyzingSpot.getStatus()).isEqualTo(StyleSpotStatus.ANALYZING);
		assertThat(analyzingSpot.getAnalysisAttempt()).isEqualTo(currentPreparation.analysisAttempt());

		StyleResultResponse completed = styleAnalysisTransactionService.complete(currentPreparation, decision);

		assertThat(completed.passportSessionId()).isEqualTo(passportSession.getId());
		assertThat(styleResultRepository.count()).isEqualTo(1);
		assertThat(styleSpotRepository.findById(STYLE_SPOT_CODE).orElseThrow().getStatus())
				.isEqualTo(StyleSpotStatus.RESULT);
	}

	private PassportSession createJourneyData() {
		PassportSession passportSession = createJourneyDataWithoutProductTag();
		Product product = productRepository.saveAndFlush(Product.create(
				RecommendedProduct.STARK_BACKPACK.name(),
				RecommendedProduct.STARK_BACKPACK.getDisplayName()
		));
		productTagRepository.saveAndFlush(ProductTag.create(passportSession, product));
		return passportSession;
	}

	private PassportSession createJourneyDataWithoutProductTag() {
		PassportSession passportSession = saveReadyToBoardSession();
		journeyResponseRepository.saveAll(List.of(
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
		));
		journeyStampRepository.saveAll(List.of(
				stamp(passportSession, "ORIGIN_GATE"),
				stamp(passportSession, "MATERIAL_LOUNGE"),
				stamp(passportSession, "MOVEMENT_DECK"),
				stamp(passportSession, "CITY_MOOD_ROOM")
		));
		return passportSession;
	}

	private PassportSession reloadSession(PassportSession passportSession) {
		return passportSessionRepository.findById(passportSession.getId()).orElseThrow();
	}

	private PassportSession saveExploringSession() {
		PassportCard card = savePassportCard();
		return passportSessionRepository.saveAndFlush(PassportSession.start(card));
	}

	private PassportSession saveReadyToBoardSession() {
		PassportCard card = savePassportCard();
		return passportSessionRepository.saveAndFlush(PassportSession.readyToBoard(card));
	}

	private PassportCard savePassportCard() {
		return passportCardRepository.saveAndFlush(PassportCard.issue("TEST-" + UUID.randomUUID()));
	}

	private JourneyStamp stamp(PassportSession passportSession, String spotCode) {
		JourneySpot journeySpot = journeySpotRepository.findByCode(spotCode).orElseThrow();
		return JourneyStamp.create(passportSession, journeySpot);
	}

	private StyleAnalysisDecision validAnalysisDecision() {
		return new StyleAnalysisDecision(
				new ValidatedStyleAnalysis(
						CityCode.BERLIN_AFTERDARK_NOMAD,
						RecommendedProduct.STARK_BACKPACK,
						StyleMood.AFTERDARK_MOVEMENT,
						CityBackground.BERLIN_AFTER_DARK,
						"A valid result for analysis attempt regression testing.",
						92
				),
				false
		);
	}

	private void cleanDatabase() {
		journeySouvenirRepository.deleteAll();
		journeySouvenirRepository.flush();
		styleResultRepository.deleteAllInBatch();
		styleSpotRepository.deleteAllInBatch();
		productTagRepository.deleteAllInBatch();
		journeyStampRepository.deleteAllInBatch();
		journeyResponseRepository.deleteAllInBatch();
		productRepository.deleteAllInBatch();
		passportSessionRepository.deleteAllInBatch();
		passportCardRepository.deleteAllInBatch();
	}
}
