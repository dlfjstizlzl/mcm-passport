package com.mcm.passport.domain.journey.config;

import com.mcm.passport.domain.journey.entity.GuideOption;
import com.mcm.passport.domain.journey.entity.GuideQuestion;
import com.mcm.passport.domain.journey.entity.JourneySpot;
import com.mcm.passport.domain.journey.repository.GuideOptionRepository;
import com.mcm.passport.domain.journey.repository.GuideQuestionRepository;
import com.mcm.passport.domain.journey.repository.JourneySpotRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(0)
public class JourneyGuideDataInitializer implements ApplicationRunner {

	private final JourneySpotRepository journeySpotRepository;
	private final GuideQuestionRepository guideQuestionRepository;
	private final GuideOptionRepository guideOptionRepository;

	public JourneyGuideDataInitializer(
			JourneySpotRepository journeySpotRepository,
			GuideQuestionRepository guideQuestionRepository,
			GuideOptionRepository guideOptionRepository
	) {
		this.journeySpotRepository = journeySpotRepository;
		this.guideQuestionRepository = guideQuestionRepository;
		this.guideOptionRepository = guideOptionRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		JourneySpot originGate = spot(
				"ORIGIN_GATE",
				"Origin Gate",
				"Origin Gate는 고객님께서 MCM의 여행, 이동, 도시성, 헤리티지 감성을 처음 경험하시는 공간입니다.",
				1
		);
		question(originGate, new QuestionSeed(
				"JOURNEY_START_MOOD",
				"오늘은 어떤 무드로 여정을 시작하고 싶으신가요?",
				1,
				List.of(
						new OptionSeed("REFINED_ELEGANT", "세련되고 우아한", 1),
						new OptionSeed("TREND_SENSORY", "트렌디하고 감각적인", 2),
						new OptionSeed("FREE_NATURAL", "자유롭고 내추럴한", 3),
						new OptionSeed("MODERN_CHIC", "모던하고 시크한", 4)
				)
		));
		question(originGate, new QuestionSeed(
				"BRAND_SENSATION",
				"방금 들은 MCM의 이야기 중 고객님에게 가장 끌리는 감각은 무엇인가요?",
				2,
				List.of(
						new OptionSeed("FREE_MOVEMENT", "자유롭게 이동하는 감각", 1),
						new OptionSeed("URBAN_CONFIDENCE", "도시적인 자신감", 2),
						new OptionSeed("CLASSIC_HERITAGE", "클래식한 헤리티지", 3),
						new OptionSeed("DISCOVERY_EXCITEMENT", "새로운 장소를 발견하는 설렘", 4)
				)
		));

		JourneySpot materialLounge = spot(
				"MATERIAL_LOUNGE",
				"Material Lounge",
				"Material Lounge는 고객님께서 MCM 제품의 소재, 패턴, 컬러, 질감을 가까이 보고 만져보시는 공간입니다.",
				2
		);
		question(materialLounge, new QuestionSeed(
				"MATERIAL_SENSATION",
				"가장 먼저 시선이 머무른 소재의 감각은 무엇인가요?",
				1,
				List.of(
						new OptionSeed("SUBTLE_GLOSS", "은은하게 빛나는 광택", 1),
						new OptionSeed("SOFT_CALM_TOUCH", "부드럽고 차분한 촉감", 2),
						new OptionSeed("SOLID_STRUCTURE", "단단하고 구조적인 형태", 3),
						new OptionSeed("ICONIC_PATTERN", "한눈에 보이는 아이코닉한 패턴", 4)
				)
		));
		question(materialLounge, new QuestionSeed(
				"COLOR_PATTERN_MOOD",
				"오늘 더 끌리는 컬러와 패턴 무드는 무엇인가요?",
				2,
				List.of(
						new OptionSeed("BLACK_DARK_TONE", "블랙과 다크 톤", 1),
						new OptionSeed("BROWN_CLASSIC_PATTERN", "브라운과 클래식 패턴", 2),
						new OptionSeed("LIGHT_NEUTRAL_TONE", "밝고 부드러운 뉴트럴 톤", 3),
						new OptionSeed("POINT_COLOR_GRAPHIC", "포인트가 되는 컬러나 그래픽", 4)
				)
		));

		JourneySpot movementDeck = spot(
				"MOVEMENT_DECK",
				"Movement Deck",
				"Movement Deck은 고객님께서 제품을 직접 들어보고, 움직여보며 실제 생활 장면 속 어울림을 확인하시는 공간입니다.",
				3
		);
		question(movementDeck, new QuestionSeed(
				"WEARING_SCENE",
				"이 가방을 들었을 때 가장 자연스럽게 떠오르는 장면은 무엇인가요?",
				1,
				List.of(
						new OptionSeed("URBAN_COMMUTE", "출근길이나 학교 가는 도시 이동", 1),
						new OptionSeed("WEEKEND_TRIP", "주말의 짧은 여행", 2),
						new OptionSeed("CREATIVE_VISIT", "전시, 팝업, 카페 방문", 3),
						new OptionSeed("NIGHT_OCCASION", "밤의 약속이나 특별한 자리", 4),
						new OptionSeed("AIRPORT_HOTEL", "공항과 호텔 사이를 오가는 이동", 5)
				)
		));

		JourneySpot cityMoodRoom = spot(
				"CITY_MOOD_ROOM",
				"City Mood Room",
				"City Mood Room은 Guide가 해석한 도시 무드를 통해 고객님께서 스타일 감각을 확인하시는 공간입니다.",
				4
		);
		question(cityMoodRoom, new QuestionSeed(
				"CITY_MOOD_SIGNAL",
				"방금 경험한 도시 무드 중 가장 강하게 남은 감각은 무엇인가요?",
				1,
				List.of(
						new OptionSeed("BERLIN_AFTER_DARK", "낮은 조명과 어두운 분위기", 1),
						new OptionSeed("SEOUL_PULSE", "빠른 리듬과 에너지", 2),
						new OptionSeed("TOKYO_QUIET", "고요한 선과 정제된 분위기", 3),
						new OptionSeed("NOMAD_DISCOVERY", "낯선 공간에서 느껴지는 자유로움", 4),
						new OptionSeed("MUNICH_HERITAGE", "클래식하고 깊이 있는 질감", 5),
						new OptionSeed("NEW_YORK_GRAPHIC", "강한 그래픽과 선명한 이미지", 6)
				)
		));
	}

	private JourneySpot spot(String code, String name, String description, int sequence) {
		JourneySpot journeySpot = journeySpotRepository.findByCode(code)
				.orElseGet(() -> journeySpotRepository.save(
						JourneySpot.create(code, name, description, sequence, true)
				));
		journeySpot.updateDescription(description);
		return journeySpot;
	}

	private void question(JourneySpot journeySpot, QuestionSeed seed) {
		GuideQuestion question = guideQuestionRepository
				.findByJourneySpot_IdAndCode(journeySpot.getId(), seed.code())
				.orElseGet(() -> guideQuestionRepository.save(GuideQuestion.create(
						journeySpot,
						seed.code(),
						seed.text(),
						true,
						seed.sequence()
				)));

		for (OptionSeed option : seed.options()) {
			guideOptionRepository.findByGuideQuestion_IdAndCode(question.getId(), option.code())
					.orElseGet(() -> guideOptionRepository.save(GuideOption.create(
							question,
							option.code(),
							option.label(),
							option.sequence()
					)));
		}
	}

	private record QuestionSeed(String code, String text, int sequence, List<OptionSeed> options) {
	}

	private record OptionSeed(String code, String label, int sequence) {
	}
}
