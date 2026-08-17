package com.mcm.passport.domain.style.service;

import com.mcm.passport.domain.journey.service.JourneyDataReader;
import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.style.dto.JourneySouvenirResponse;
import com.mcm.passport.domain.style.dto.MyPassportResponse;
import com.mcm.passport.domain.style.dto.StylePortraitResponse;
import com.mcm.passport.domain.style.dto.StyleResultResponse;
import com.mcm.passport.domain.style.repository.JourneySouvenirRepository;
import com.mcm.passport.domain.style.repository.StylePortraitRepository;
import com.mcm.passport.domain.style.repository.StyleResultRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyPassportService {

	private final PassportSessionRepository passportSessionRepository;
	private final JourneyDataReader journeyDataReader;
	private final StyleResultRepository styleResultRepository;
	private final StylePortraitRepository stylePortraitRepository;
	private final JourneySouvenirRepository journeySouvenirRepository;

	public MyPassportService(
			PassportSessionRepository passportSessionRepository,
			JourneyDataReader journeyDataReader,
			StyleResultRepository styleResultRepository,
			StylePortraitRepository stylePortraitRepository,
			JourneySouvenirRepository journeySouvenirRepository
	) {
		this.passportSessionRepository = passportSessionRepository;
		this.journeyDataReader = journeyDataReader;
		this.styleResultRepository = styleResultRepository;
		this.stylePortraitRepository = stylePortraitRepository;
		this.journeySouvenirRepository = journeySouvenirRepository;
	}

	@Transactional(readOnly = true)
	public MyPassportResponse get(Long passportSessionId) {
		PassportSession passportSession = passportSessionRepository.findById(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND));
		JourneyDataSnapshot journey = journeyDataReader.read(passportSessionId);
		StyleResultResponse styleResult = styleResultRepository.findByPassportSessionId(passportSessionId)
				.map(StyleResultResponse::from)
				.orElse(null);
		StylePortraitResponse portrait = stylePortraitRepository.findByPassportSessionId(passportSessionId)
				.map(StylePortraitResponse::from)
				.orElse(null);
		JourneySouvenirResponse souvenir = journeySouvenirRepository.findByPassportSessionId(passportSessionId)
				.map(JourneySouvenirResponse::from)
				.orElse(null);

		return new MyPassportResponse(
				passportSession.getId(),
				passportSession.getPassportCard().getCardUid(),
				passportSession.getStatus(),
				passportSession.getStartedAt(),
				passportSession.getCompletedAt(),
				journey,
				styleResult,
				portrait,
				souvenir
		);
	}
}
