package com.mcm.passport.domain.style.service;

import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.style.dto.StyleSpotResponse;
import com.mcm.passport.domain.style.entity.StyleSpot;
import com.mcm.passport.domain.style.entity.StyleSpotStatus;
import com.mcm.passport.domain.style.repository.StyleSpotRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class StyleSpotService {

	private final StyleSpotRepository styleSpotRepository;
	private final PassportSessionRepository passportSessionRepository;

	public StyleSpotService(
			StyleSpotRepository styleSpotRepository,
			PassportSessionRepository passportSessionRepository
	) {
		this.styleSpotRepository = styleSpotRepository;
		this.passportSessionRepository = passportSessionRepository;
	}

	@Transactional
	public StyleSpotResponse connect(String spotCode, Long passportSessionId) {
		StyleSpot styleSpot = findForUpdate(spotCode);
		if (styleSpot.hasSession(passportSessionId)
				&& styleSpot.getStatus() != StyleSpotStatus.WAITING
				&& styleSpot.getStatus() != StyleSpotStatus.RESET) {
			return StyleSpotResponse.from(styleSpot);
		}

		PassportSession passportSession = passportSessionRepository.findByIdForUpdate(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND));
		styleSpot.connect(passportSession);

		return StyleSpotResponse.from(styleSpot);
	}

	@Transactional
	public StyleSpotResponse reset(String spotCode) {
		StyleSpot styleSpot = findForUpdate(spotCode);
		if (styleSpot.getStatus() == StyleSpotStatus.CONNECTED
				|| styleSpot.getStatus() == StyleSpotStatus.ANALYZING) {
			PassportSession passportSession = passportSessionRepository
					.findByIdForUpdate(styleSpot.getPassportSession().getId())
					.orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND));
			passportSession.returnToReadyToBoard();
		}
		styleSpot.reset();
		return StyleSpotResponse.from(styleSpot);
	}

	private StyleSpot findForUpdate(String spotCode) {
		return styleSpotRepository.findByCodeForUpdate(normalize(spotCode))
				.orElseThrow(() -> new BusinessException(ErrorCode.STYLE_SPOT_NOT_FOUND));
	}

	private String normalize(String spotCode) {
		return spotCode.trim().toUpperCase(Locale.ROOT);
	}
}
