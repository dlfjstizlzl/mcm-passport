package com.mcm.passport.domain.journey.service;

import com.mcm.passport.domain.journey.dto.GuideResponseRequest;
import com.mcm.passport.domain.journey.dto.GuideResponseResponse;
import com.mcm.passport.domain.journey.entity.GuideOption;
import com.mcm.passport.domain.journey.entity.GuideQuestion;
import com.mcm.passport.domain.journey.entity.GuideResponse;
import com.mcm.passport.domain.journey.repository.GuideOptionRepository;
import com.mcm.passport.domain.journey.repository.GuideQuestionRepository;
import com.mcm.passport.domain.journey.repository.GuideResponseRepository;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.entity.PassportSessionStatus;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuideResponseService {

	private final PassportSessionRepository passportSessionRepository;
	private final GuideQuestionRepository guideQuestionRepository;
	private final GuideOptionRepository guideOptionRepository;
	private final GuideResponseRepository guideResponseRepository;

	public GuideResponseService(
			PassportSessionRepository passportSessionRepository,
			GuideQuestionRepository guideQuestionRepository,
			GuideOptionRepository guideOptionRepository,
			GuideResponseRepository guideResponseRepository
	) {
		this.passportSessionRepository = passportSessionRepository;
		this.guideQuestionRepository = guideQuestionRepository;
		this.guideOptionRepository = guideOptionRepository;
		this.guideResponseRepository = guideResponseRepository;
	}

	@Transactional
	public GuideResponseResponse saveOrUpdate(
			Long passportSessionId,
			Long questionId,
			GuideResponseRequest request
	) {
		PassportSession passportSession = passportSessionRepository.findByIdForUpdate(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND));
		if (passportSession.getStatus() != PassportSessionStatus.EXPLORING) {
			throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
		}

		GuideQuestion guideQuestion = guideQuestionRepository.findByIdAndActiveTrue(questionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.GUIDE_QUESTION_NOT_FOUND));
		GuideOption guideOption = guideOptionRepository.findByIdAndActiveTrue(request.optionId())
				.orElseThrow(() -> new BusinessException(ErrorCode.GUIDE_OPTION_NOT_FOUND));

		if (!guideOption.getGuideQuestion().getId().equals(guideQuestion.getId())) {
			throw new BusinessException(ErrorCode.INVALID_GUIDE_OPTION);
		}

		GuideResponse guideResponse = guideResponseRepository
				.findByPassportSession_IdAndGuideQuestion_Id(passportSessionId, questionId)
				.map(existing -> {
					existing.changeOption(guideOption);
					return existing;
				})
				.orElseGet(() -> guideResponseRepository.save(
						GuideResponse.create(passportSession, guideQuestion, guideOption)
				));

		return GuideResponseResponse.from(guideResponse);
	}
}
