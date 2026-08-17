package com.mcm.passport.domain.style;

import com.mcm.passport.domain.journey.repository.GuideOptionRepository;
import com.mcm.passport.domain.journey.repository.GuideQuestionRepository;
import com.mcm.passport.domain.journey.repository.GuideResponseRepository;
import com.mcm.passport.domain.journey.repository.JourneySpotRepository;
import com.mcm.passport.domain.journey.repository.JourneyStampRepository;
import com.mcm.passport.domain.journey.service.JourneyDataReader;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.entity.PassportSessionStatus;
import com.mcm.passport.domain.passport.repository.PassportCardRepository;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.product.repository.ProductRepository;
import com.mcm.passport.domain.product.repository.ProductTagRepository;
import com.mcm.passport.domain.style.analysis.StyleAnalysisDecision;
import com.mcm.passport.domain.style.analysis.ValidatedStyleAnalysis;
import com.mcm.passport.domain.style.dto.JourneySouvenirResponse;
import com.mcm.passport.domain.style.dto.StyleResultResponse;
import com.mcm.passport.domain.style.dto.StyleSpotConnectResponse;
import com.mcm.passport.domain.style.dto.StyleSpotSessionResponse;
import com.mcm.passport.domain.style.entity.CityBackground;
import com.mcm.passport.domain.style.entity.CityCode;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import com.mcm.passport.domain.style.entity.StyleMood;
import com.mcm.passport.domain.style.entity.StyleSpot;
import com.mcm.passport.domain.style.entity.StyleSpotSessionStatus;
import com.mcm.passport.domain.style.entity.StyleSpotStatus;
import com.mcm.passport.domain.style.repository.JourneySouvenirRepository;
import com.mcm.passport.domain.style.repository.StylePortraitRepository;
import com.mcm.passport.domain.style.repository.StyleResultRepository;
import com.mcm.passport.domain.style.repository.StyleSpotRepository;
import com.mcm.passport.domain.style.repository.StyleSpotSessionRepository;
import com.mcm.passport.domain.style.service.JourneySouvenirCreation;
import com.mcm.passport.domain.style.service.JourneySouvenirService;
import com.mcm.passport.domain.style.service.StyleAnalysisPreparation;
import com.mcm.passport.domain.style.service.StyleAnalysisTransactionService;
import com.mcm.passport.domain.style.service.StyleSpotFlowService;
import com.mcm.passport.domain.style.service.StyleSpotService;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
		"mcm.style.analysis.provider=mock",
		"mcm.reference-data.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:style-flow-integrated;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class StyleFlowIntegrationTest {

	private static final String STYLE_SPOT_CODE = "GATE-S1";

	@Autowired private StyleSpotService styleSpotService;
	@Autowired private StyleSpotFlowService styleSpotFlowService;
	@Autowired private StyleAnalysisTransactionService analysisTransactionService;
	@Autowired private JourneySouvenirService journeySouvenirService;
	@Autowired private JourneyDataReader journeyDataReader;
	@Autowired private PassportSessionRepository passportSessionRepository;
	@Autowired private PassportCardRepository passportCardRepository;
	@Autowired private JourneySpotRepository journeySpotRepository;
	@Autowired private GuideQuestionRepository guideQuestionRepository;
	@Autowired private GuideOptionRepository guideOptionRepository;
	@Autowired private GuideResponseRepository guideResponseRepository;
	@Autowired private JourneyStampRepository journeyStampRepository;
	@Autowired private ProductRepository productRepository;
	@Autowired private ProductTagRepository productTagRepository;
	@Autowired private StyleSpotRepository styleSpotRepository;
	@Autowired private StyleSpotSessionRepository styleSpotSessionRepository;
	@Autowired private StyleResultRepository styleResultRepository;
	@Autowired private StylePortraitRepository stylePortraitRepository;
	@Autowired private JourneySouvenirRepository journeySouvenirRepository;

	private IntegratedStyleTestFixture fixture;

	@BeforeEach
	void setUp() {
		cleanDatabase();
		styleSpotRepository.saveAndFlush(StyleSpot.waiting(STYLE_SPOT_CODE));
		fixture = new IntegratedStyleTestFixture(
				passportCardRepository, passportSessionRepository, journeySpotRepository,
				guideQuestionRepository, guideOptionRepository, guideResponseRepository,
				journeyStampRepository, productRepository, productTagRepository
		);
	}

	@Test
	void connectTriggersAnalysisFromActualGuideResponsesAndCompletesWithSouvenir() {
		PassportSession session = fixture.readyToBoardWithJourney(true);

		StyleSpotConnectResponse connected = styleSpotFlowService.connectAndAnalyze(STYLE_SPOT_CODE, session.getId());

		assertThat(connected.styleSpotSessionId()).isNotNull();
		assertThat(connected.status()).isEqualTo(StyleSpotStatus.RESULT);
		assertThat(connected.styleResult().cityCode()).isEqualTo(CityCode.BERLIN_AFTERDARK_NOMAD.name());
		assertThat(connected.styleResult().recommendedProductCode()).isEqualTo(RecommendedProduct.STARK_BACKPACK.name());
		assertThat(connected.styleResult().matchScore()).isEqualTo(91);
		assertThat(connected.styleResult().usedFallback()).isFalse();
		assertThat(reload(session).getStatus()).isEqualTo(PassportSessionStatus.STYLE_SPOT);
		assertThat(styleResultRepository.count()).isEqualTo(1);

		JourneySouvenirCreation creation = journeySouvenirService.create(session.getId());
		JourneySouvenirResponse souvenir = creation.souvenir();

		assertThat(creation.created()).isTrue();
		assertThat(souvenir.journeyStamps()).containsExactly(
				"ORIGIN_GATE", "MATERIAL_LOUNGE", "MOVEMENT_DECK", "CITY_MOOD_ROOM"
		);
		assertThat(souvenir.taggedProductCodes()).containsExactly(RecommendedProduct.STARK_BACKPACK.name());
		assertThat(reload(session).getStatus()).isEqualTo(PassportSessionStatus.COMPLETED);

		StyleSpotSessionResponse disconnected = styleSpotService.disconnect(connected.styleSpotSessionId());
		assertThat(disconnected.status()).isEqualTo(StyleSpotSessionStatus.DISCONNECTED);
		assertThat(styleSpotRepository.findById(STYLE_SPOT_CODE).orElseThrow().getStatus())
				.isEqualTo(StyleSpotStatus.RESET);
		assertThat(reload(session).getStatus()).isEqualTo(PassportSessionStatus.COMPLETED);
	}

	@Test
	void analyzesActualGuideResponsesWithoutOptionalProductTag() {
		PassportSession session = fixture.readyToBoardWithJourney(false);

		StyleSpotConnectResponse response = styleSpotFlowService.connectAndAnalyze(STYLE_SPOT_CODE, session.getId());

		assertThat(response.styleResult().id()).isNotNull();
		assertThat(response.styleResult().matchScore()).isEqualTo(90);
		assertThat(productTagRepository.findAllByPassportSession_IdOrderByIdAsc(session.getId())).isEmpty();
		assertThat(reload(session).getStatus()).isEqualTo(PassportSessionStatus.STYLE_SPOT);
	}

	@Test
	void mapsBe1GuideResponsesInJourneyOrderWithOptionCodesAndLabels() {
		PassportSession session = fixture.readyToBoardWithJourney(true);

		var snapshot = journeyDataReader.read(session.getId());

		assertThat(snapshot.responses())
				.extracting(response -> response.spotCode() + ":" + response.questionCode()
						+ ":" + response.answerCode())
				.containsExactly(
						"MOVEMENT_DECK:WEARING_SCENE:WEEKEND_TRIP",
						"CITY_MOOD_ROOM:CITY_MOOD_SIGNAL:BERLIN_AFTER_DARK"
				);
		assertThat(snapshot.responses()).allSatisfy(response -> assertThat(response.answerText()).isNotBlank());
		assertThat(snapshot.stamps()).extracting(stamp -> stamp.spotCode()).containsExactly(
				"ORIGIN_GATE", "MATERIAL_LOUNGE", "MOVEMENT_DECK", "CITY_MOOD_ROOM"
		);
		assertThat(snapshot.taggedProducts()).singleElement()
				.satisfies(product -> assertThat(product.code()).isEqualTo(RecommendedProduct.STARK_BACKPACK.name()));
	}

	@Test
	void rejectsExploringSessionAndJourneyWithoutSignals() {
		PassportSession exploring = fixture.exploring();

		assertThatThrownBy(() -> styleSpotFlowService.connectAndAnalyze(STYLE_SPOT_CODE, exploring.getId()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_SESSION_STATUS));

		PassportSession noJourney = fixture.readyToBoardWithoutJourney();
		assertThatThrownBy(() -> styleSpotFlowService.connectAndAnalyze(STYLE_SPOT_CODE, noJourney.getId()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.JOURNEY_NOT_COMPLETED));
		assertThat(styleSpotRepository.findById(STYLE_SPOT_CODE).orElseThrow().getStatus())
				.isEqualTo(StyleSpotStatus.CONNECTED);
		Long failedConnectionId = styleSpotSessionRepository.findActiveByStyleSpotCode(STYLE_SPOT_CODE)
				.orElseThrow().getId();
		styleSpotService.disconnect(failedConnectionId);
		assertThat(reload(noJourney).getStatus()).isEqualTo(PassportSessionStatus.READY_TO_BOARD);
	}

	@Test
	void disconnectBeforeResultIsIdempotentAndAllowsRetry() {
		PassportSession session = fixture.readyToBoardWithJourney(false);
		StyleSpotSessionResponse connection = styleSpotService.connectSession(STYLE_SPOT_CODE, session.getId());

		StyleSpotSessionResponse first = styleSpotService.disconnect(connection.id());
		StyleSpotSessionResponse second = styleSpotService.disconnect(connection.id());

		assertThat(first.status()).isEqualTo(StyleSpotSessionStatus.DISCONNECTED);
		assertThat(first.disconnectedAt()).isNotNull();
		assertThat(second.id()).isEqualTo(first.id());
		assertThat(second.disconnectedAt()).isNotNull();
		assertThat(reload(session).getStatus()).isEqualTo(PassportSessionStatus.READY_TO_BOARD);
		assertThat(styleSpotService.connectSession(STYLE_SPOT_CODE, session.getId()).id()).isNotEqualTo(connection.id());
	}

	@Test
	void allowsOnlyOneActiveConnectionPerStyleSpot() {
		PassportSession first = fixture.readyToBoardWithJourney(false);
		PassportSession second = fixture.readyToBoardWithJourney(false);
		styleSpotService.connectSession(STYLE_SPOT_CODE, first.getId());

		assertThatThrownBy(() -> styleSpotService.connectSession(STYLE_SPOT_CODE, second.getId()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STYLE_SPOT_IN_USE));
		assertThat(styleSpotSessionRepository.findActiveByStyleSpotCode(STYLE_SPOT_CODE))
				.hasValueSatisfying(connection ->
						assertThat(connection.getPassportSession().getId()).isEqualTo(first.getId()));
		assertThat(reload(second).getStatus()).isEqualTo(PassportSessionStatus.READY_TO_BOARD);
	}

	@Test
	void staleAnalysisAttemptCannotOverwriteNewConnectionResult() {
		PassportSession session = fixture.readyToBoardWithJourney(false);
		styleSpotService.connectSession(STYLE_SPOT_CODE, session.getId());
		StyleAnalysisPreparation stale = analysisTransactionService.prepare(STYLE_SPOT_CODE);

		styleSpotService.reset(STYLE_SPOT_CODE);
		styleSpotService.connectSession(STYLE_SPOT_CODE, session.getId());
		StyleAnalysisPreparation current = analysisTransactionService.prepare(STYLE_SPOT_CODE);

		assertThat(current.analysisAttempt()).isGreaterThan(stale.analysisAttempt());
		assertThatThrownBy(() -> analysisTransactionService.complete(stale, validDecision()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_SESSION_STATUS));
		analysisTransactionService.fail(stale);
		assertThat(styleSpotRepository.findById(STYLE_SPOT_CODE).orElseThrow().getStatus())
				.isEqualTo(StyleSpotStatus.ANALYZING);

		StyleResultResponse completed = analysisTransactionService.complete(current, validDecision());
		assertThat(completed.passportSessionId()).isEqualTo(session.getId());
		assertThat(styleResultRepository.count()).isEqualTo(1);
	}

	private PassportSession reload(PassportSession session) {
		return passportSessionRepository.findById(session.getId()).orElseThrow();
	}

	private StyleAnalysisDecision validDecision() {
		return new StyleAnalysisDecision(
				new ValidatedStyleAnalysis(
						CityCode.BERLIN_AFTERDARK_NOMAD,
						RecommendedProduct.STARK_BACKPACK,
						StyleMood.AFTERDARK_MOVEMENT,
						CityBackground.BERLIN_AFTER_DARK,
						"A valid integrated analysis result.",
						92
				),
				false
		);
	}

	private void cleanDatabase() {
		stylePortraitRepository.deleteAllInBatch();
		journeySouvenirRepository.deleteAll();
		journeySouvenirRepository.flush();
		styleResultRepository.deleteAllInBatch();
		styleSpotSessionRepository.deleteAllInBatch();
		styleSpotRepository.deleteAllInBatch();
		productTagRepository.deleteAllInBatch();
		guideResponseRepository.deleteAllInBatch();
		journeyStampRepository.deleteAllInBatch();
		passportSessionRepository.deleteAllInBatch();
		passportCardRepository.deleteAllInBatch();
	}
}
