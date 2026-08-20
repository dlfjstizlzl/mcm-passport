package com.mcm.passport.domain.style.service;

import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.entity.PassportSessionStatus;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.style.dto.StyleSpotResponse;
import com.mcm.passport.domain.style.dto.StyleSpotSessionResponse;
import com.mcm.passport.domain.style.entity.StyleSpot;
import com.mcm.passport.domain.style.entity.StyleSpotSession;
import com.mcm.passport.domain.style.entity.StyleSpotStatus;
import com.mcm.passport.domain.style.repository.StyleSpotRepository;
import com.mcm.passport.domain.style.repository.StyleSpotSessionRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
public class StyleSpotService {

	private final StyleSpotRepository styleSpotRepository;
	private final StyleSpotSessionRepository styleSpotSessionRepository;
	private final PassportSessionRepository passportSessionRepository;

	public StyleSpotService(
			StyleSpotRepository styleSpotRepository,
			StyleSpotSessionRepository styleSpotSessionRepository,
			PassportSessionRepository passportSessionRepository
	) {
		this.styleSpotRepository = styleSpotRepository;
		this.styleSpotSessionRepository = styleSpotSessionRepository;
		this.passportSessionRepository = passportSessionRepository;
	}

	@Transactional
	public StyleSpotSessionResponse connectSession(String styleSpotId, Long passportSessionId) {
		StyleSpot styleSpot = findForUpdate(styleSpotId);
		StyleSpotSession activeSession = findActive(styleSpot.getCode());
		if (activeSession != null) {
			if (Objects.equals(activeSession.getPassportSession().getId(), passportSessionId)) {
				return StyleSpotSessionResponse.from(activeSession);
			}
			if (styleSpot.getStatus() != StyleSpotStatus.RESULT) {
				throw new BusinessException(ErrorCode.STYLE_SPOT_IN_USE);
			}
			// A displayed result belongs to a finished interaction. Release it automatically
			// so the next visitor can use the physical Style Spot without a manual reset.
			resetActiveConnection(styleSpot, activeSession);
			styleSpotSessionRepository.flush();
		}

		PassportSession passportSession = passportSessionRepository.findByIdForUpdate(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND));
		passportSession.enterStyleSpot();
		styleSpot.connect();
		StyleSpotSession connection = styleSpotSessionRepository.save(
				StyleSpotSession.connect(styleSpot, passportSession)
		);
		return StyleSpotSessionResponse.from(connection);
	}

	/** Legacy prototype service entrypoint. New flows use connectSession via StyleSpotFlowService. */
	@Transactional
	public StyleSpotResponse connect(String styleSpotId, Long passportSessionId) {
		connectSession(styleSpotId, passportSessionId);
		return display(styleSpotId);
	}

	@Transactional(readOnly = true)
	public StyleSpotResponse display(String styleSpotId) {
		StyleSpot styleSpot = find(styleSpotId);
		return StyleSpotResponse.from(styleSpot, findActive(styleSpot.getCode()));
	}

	@Transactional
	public StyleSpotSessionResponse disconnect(Long styleSpotSessionId) {
		StyleSpotSession snapshot = styleSpotSessionRepository.findById(styleSpotSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.STYLE_SPOT_SESSION_NOT_FOUND));
		StyleSpot styleSpot = findForUpdate(snapshot.getStyleSpot().getCode());
		StyleSpotSession connection = styleSpotSessionRepository.findByIdForUpdate(styleSpotSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.STYLE_SPOT_SESSION_NOT_FOUND));
		if (!connection.isActive()) {
			return StyleSpotSessionResponse.from(connection);
		}

		StyleSpotSession activeSession = findActive(styleSpot.getCode());
		if (activeSession == null || !Objects.equals(activeSession.getId(), connection.getId())) {
			throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
		}

		PassportSession passportSession = passportSessionRepository
				.findByIdForUpdate(connection.getPassportSession().getId())
				.orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND));
		if (passportSession.getStatus() == PassportSessionStatus.STYLE_SPOT
				&& styleSpot.getStatus() != StyleSpotStatus.RESULT) {
			passportSession.returnToReadyToBoard();
		}
		connection.disconnect();
		styleSpot.reset();
		return StyleSpotSessionResponse.from(connection);
	}

	@Transactional
	public StyleSpotResponse reset(String styleSpotId) {
		StyleSpot styleSpot = findForUpdate(styleSpotId);
		StyleSpotSession activeSession = findActive(styleSpot.getCode());
		if (activeSession != null) {
			return resetActiveConnection(styleSpot, activeSession);
		}
		styleSpot.reset();
		return StyleSpotResponse.from(styleSpot, null);
	}

	private StyleSpotResponse resetActiveConnection(StyleSpot styleSpot, StyleSpotSession connection) {
		PassportSession passportSession = passportSessionRepository
				.findByIdForUpdate(connection.getPassportSession().getId())
				.orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND));
		if (passportSession.getStatus() == PassportSessionStatus.STYLE_SPOT
				&& styleSpot.getStatus() != StyleSpotStatus.RESULT) {
			passportSession.returnToReadyToBoard();
		}
		connection.disconnect();
		styleSpot.reset();
		return StyleSpotResponse.from(styleSpot, null);
	}

	private StyleSpot findForUpdate(String styleSpotId) {
		return styleSpotRepository.findByCodeForUpdate(normalize(styleSpotId))
				.orElseThrow(() -> new BusinessException(ErrorCode.STYLE_SPOT_NOT_FOUND));
	}

	private StyleSpot find(String styleSpotId) {
		return styleSpotRepository.findById(normalize(styleSpotId))
				.orElseThrow(() -> new BusinessException(ErrorCode.STYLE_SPOT_NOT_FOUND));
	}

	private StyleSpotSession findActive(String styleSpotId) {
		return styleSpotSessionRepository.findActiveByStyleSpotCode(styleSpotId).orElse(null);
	}

	private String normalize(String styleSpotId) {
		return styleSpotId.trim().toUpperCase(Locale.ROOT);
	}
}
