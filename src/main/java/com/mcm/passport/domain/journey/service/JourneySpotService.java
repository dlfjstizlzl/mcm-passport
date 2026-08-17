package com.mcm.passport.domain.journey.service;

import com.mcm.passport.domain.journey.dto.JourneySpotDetailResponse;
import com.mcm.passport.domain.journey.dto.JourneySpotResponse;
import com.mcm.passport.domain.journey.entity.GuideQuestion;
import com.mcm.passport.domain.journey.entity.JourneySpot;
import com.mcm.passport.domain.journey.repository.GuideOptionRepository;
import com.mcm.passport.domain.journey.repository.GuideQuestionRepository;
import com.mcm.passport.domain.journey.repository.JourneySpotRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class JourneySpotService {

	private final JourneySpotRepository journeySpotRepository;
	private final GuideQuestionRepository guideQuestionRepository;
	private final GuideOptionRepository guideOptionRepository;

	public JourneySpotService(
			JourneySpotRepository journeySpotRepository,
			GuideQuestionRepository guideQuestionRepository,
			GuideOptionRepository guideOptionRepository
	) {
		this.journeySpotRepository = journeySpotRepository;
		this.guideQuestionRepository = guideQuestionRepository;
		this.guideOptionRepository = guideOptionRepository;
	}

	public List<JourneySpotResponse> getAll() {
		return journeySpotRepository.findAllByActiveTrueOrderBySequenceAsc()
				.stream()
				.map(JourneySpotResponse::from)
				.toList();
	}

	public JourneySpotDetailResponse get(Long journeySpotId) {
		JourneySpot journeySpot = journeySpotRepository.findByIdAndActiveTrue(journeySpotId)
				.orElseThrow(() -> new BusinessException(ErrorCode.JOURNEY_SPOT_NOT_FOUND));

		List<JourneySpotDetailResponse.QuestionResponse> questions = guideQuestionRepository
				.findAllByJourneySpot_IdAndActiveTrueOrderBySequenceAsc(journeySpotId)
				.stream()
				.map(this::toQuestionResponse)
				.toList();

		return JourneySpotDetailResponse.of(journeySpot, questions);
	}

	private JourneySpotDetailResponse.QuestionResponse toQuestionResponse(GuideQuestion question) {
		List<JourneySpotDetailResponse.OptionResponse> options = guideOptionRepository
				.findAllByGuideQuestion_IdAndActiveTrueOrderBySequenceAsc(question.getId())
				.stream()
				.map(JourneySpotDetailResponse.OptionResponse::from)
				.toList();
		return JourneySpotDetailResponse.QuestionResponse.of(question, options);
	}
}
