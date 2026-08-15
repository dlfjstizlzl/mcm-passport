package com.mcm.passport.domain.style.service;

import com.mcm.passport.domain.journey.service.JourneyDataReader;
import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.style.dto.JourneySouvenirResponse;
import com.mcm.passport.domain.style.entity.JourneySouvenir;
import com.mcm.passport.domain.style.entity.StyleResult;
import com.mcm.passport.domain.style.repository.JourneySouvenirRepository;
import com.mcm.passport.domain.style.repository.StyleResultRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JourneySouvenirService {

	private final PassportSessionRepository passportSessionRepository;
	private final StyleResultRepository styleResultRepository;
	private final JourneySouvenirRepository journeySouvenirRepository;
	private final JourneyDataReader journeyDataReader;

	public JourneySouvenirService(
			PassportSessionRepository passportSessionRepository,
			StyleResultRepository styleResultRepository,
			JourneySouvenirRepository journeySouvenirRepository,
			JourneyDataReader journeyDataReader
	) {
		this.passportSessionRepository = passportSessionRepository;
		this.styleResultRepository = styleResultRepository;
		this.journeySouvenirRepository = journeySouvenirRepository;
		this.journeyDataReader = journeyDataReader;
	}

	@Transactional
	public JourneySouvenirCreation create(Long passportSessionId) {
		PassportSession passportSession = passportSessionRepository.findByIdForUpdate(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND));
		return journeySouvenirRepository.findByPassportSessionId(passportSessionId)
				.map(souvenir -> new JourneySouvenirCreation(JourneySouvenirResponse.from(souvenir), false))
				.orElseGet(() -> createNew(passportSession));
	}

	@Transactional(readOnly = true)
	public JourneySouvenirResponse get(Long passportSessionId) {
		JourneySouvenir souvenir = journeySouvenirRepository.findByPassportSessionId(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.JOURNEY_SOUVENIR_NOT_FOUND));
		return JourneySouvenirResponse.from(souvenir);
	}

	private JourneySouvenirCreation createNew(PassportSession passportSession) {
		Long passportSessionId = passportSession.getId();
		StyleResult styleResult = styleResultRepository.findByPassportSessionId(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.STYLE_RESULT_NOT_FOUND));
		JourneyDataSnapshot journeyData = journeyDataReader.read(passportSessionId);
		JourneySouvenir souvenir = JourneySouvenir.create(passportSession, styleResult, journeyData);

		journeySouvenirRepository.save(souvenir);
		passportSession.complete();

		return new JourneySouvenirCreation(JourneySouvenirResponse.from(souvenir), true);
	}
}
