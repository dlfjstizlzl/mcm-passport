package com.mcm.passport.domain.journey.service;

import com.mcm.passport.domain.journey.dto.JourneyStampResponse;
import com.mcm.passport.domain.journey.entity.GuideQuestion;
import com.mcm.passport.domain.journey.entity.JourneySpot;
import com.mcm.passport.domain.journey.entity.JourneyStamp;
import com.mcm.passport.domain.journey.repository.GuideQuestionRepository;
import com.mcm.passport.domain.journey.repository.GuideResponseRepository;
import com.mcm.passport.domain.journey.repository.JourneySpotRepository;
import com.mcm.passport.domain.journey.repository.JourneyStampRepository;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.entity.PassportSessionStatus;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JourneyStampService {

	private final PassportSessionRepository passportSessionRepository;
	private final JourneySpotRepository journeySpotRepository;
	private final GuideQuestionRepository guideQuestionRepository;
	private final GuideResponseRepository guideResponseRepository;
	private final JourneyStampRepository journeyStampRepository;

	public JourneyStampService(
			PassportSessionRepository passportSessionRepository,
			JourneySpotRepository journeySpotRepository,
			GuideQuestionRepository guideQuestionRepository,
			GuideResponseRepository guideResponseRepository,
			JourneyStampRepository journeyStampRepository
	) {
		this.passportSessionRepository = passportSessionRepository;
		this.journeySpotRepository = journeySpotRepository;
		this.guideQuestionRepository = guideQuestionRepository;
		this.guideResponseRepository = guideResponseRepository;
		this.journeyStampRepository = journeyStampRepository;
	}

	@Transactional
	public JourneyStampResponse complete(Long passportSessionId, Long journeySpotId) {
		PassportSession passportSession = passportSessionRepository.findByIdForUpdate(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND));
		if (passportSession.getStatus() != PassportSessionStatus.EXPLORING) {
			throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
		}

		JourneySpot journeySpot = journeySpotRepository.findByIdAndActiveTrue(journeySpotId)
				.orElseThrow(() -> new BusinessException(ErrorCode.JOURNEY_SPOT_NOT_FOUND));
		List<Long> requiredQuestionIds = guideQuestionRepository
				.findAllByJourneySpot_IdAndRequiredTrueAndActiveTrueOrderBySequenceAsc(journeySpotId)
				.stream()
				.map(GuideQuestion::getId)
				.toList();

		long answeredRequiredQuestions = requiredQuestionIds.isEmpty()
				? 0
				: guideResponseRepository.countByPassportSession_IdAndGuideQuestion_IdIn(
						passportSessionId,
						requiredQuestionIds
				);
		if (answeredRequiredQuestions != requiredQuestionIds.size()) {
			throw new BusinessException(ErrorCode.JOURNEY_NOT_COMPLETED);
		}
		if (journeyStampRepository.existsByPassportSession_IdAndJourneySpot_Id(
				passportSessionId,
				journeySpotId
		)) {
			throw new BusinessException(ErrorCode.JOURNEY_STAMP_ALREADY_EXISTS);
		}

		JourneyStamp journeyStamp = journeyStampRepository.save(JourneyStamp.create(passportSession, journeySpot));
		return JourneyStampResponse.from(journeyStamp);
	}
}
