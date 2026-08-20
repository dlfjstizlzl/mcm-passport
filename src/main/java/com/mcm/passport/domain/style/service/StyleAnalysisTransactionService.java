package com.mcm.passport.domain.style.service;

import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.style.analysis.StyleAnalysisDecision;
import com.mcm.passport.domain.style.dto.StyleResultResponse;
import com.mcm.passport.domain.style.entity.StyleResult;
import com.mcm.passport.domain.style.entity.StyleSpot;
import com.mcm.passport.domain.style.entity.StyleSpotSession;
import com.mcm.passport.domain.style.entity.StyleSpotStatus;
import com.mcm.passport.domain.style.repository.StyleResultRepository;
import com.mcm.passport.domain.style.repository.StyleSpotRepository;
import com.mcm.passport.domain.style.repository.StyleSpotSessionRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class StyleAnalysisTransactionService {

	private final StyleSpotRepository styleSpotRepository;
	private final StyleSpotSessionRepository styleSpotSessionRepository;
	private final StyleResultRepository styleResultRepository;
	private final StyleResultResponseMapper styleResultResponseMapper;

	public StyleAnalysisTransactionService(
			StyleSpotRepository styleSpotRepository,
			StyleSpotSessionRepository styleSpotSessionRepository,
			StyleResultRepository styleResultRepository,
			StyleResultResponseMapper styleResultResponseMapper
	) {
		this.styleSpotRepository = styleSpotRepository;
		this.styleSpotSessionRepository = styleSpotSessionRepository;
		this.styleResultRepository = styleResultRepository;
		this.styleResultResponseMapper = styleResultResponseMapper;
	}

	@Transactional
	public StyleAnalysisPreparation prepare(String spotCode) {
		StyleSpot styleSpot = findForUpdate(spotCode);
		StyleSpotSession activeSession = findActiveSession(styleSpot);
		if (styleSpot.getStatus() == StyleSpotStatus.RESULT) {
			StyleResultResponse existing = findBySession(activeSession.getPassportSession().getId());
			return new StyleAnalysisPreparation(
					styleSpot.getCode(),
					activeSession.getPassportSession().getId(),
					styleSpot.getAnalysisAttempt(),
					existing
			);
		}

		long analysisAttempt = styleSpot.startAnalysis();
		return new StyleAnalysisPreparation(
				styleSpot.getCode(),
				activeSession.getPassportSession().getId(),
				analysisAttempt,
				null
		);
	}

	@Transactional
	public StyleResultResponse complete(
			StyleAnalysisPreparation preparation,
			StyleAnalysisDecision decision
	) {
		StyleSpot styleSpot = findForUpdate(preparation.spotCode());
		StyleSpotSession activeSession = findActiveSession(styleSpot);
		if (!activeSession.getPassportSession().getId().equals(preparation.passportSessionId())
				|| !styleSpot.isCurrentAnalysisAttempt(preparation.analysisAttempt())) {
			throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
		}
		if (styleSpot.getStatus() == StyleSpotStatus.RESULT) {
			return findBySession(preparation.passportSessionId());
		}
		if (styleSpot.getStatus() != StyleSpotStatus.ANALYZING) {
			throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
		}

		PassportSession passportSession = activeSession.getPassportSession();
		StyleResult styleResult = StyleResult.create(
				passportSession,
				decision.analysis(),
				decision.usedFallback()
		);
		styleResultRepository.save(styleResult);
		styleSpot.finishAnalysis(preparation.analysisAttempt());

		return styleResultResponseMapper.from(styleResult);
	}

	@Transactional
	public void fail(StyleAnalysisPreparation preparation) {
		styleSpotRepository.findByCodeForUpdate(preparation.spotCode())
				.ifPresent(styleSpot -> styleSpotSessionRepository
						.findActiveByStyleSpotCode(styleSpot.getCode())
						.filter(session -> session.getPassportSession().getId()
								.equals(preparation.passportSessionId()))
						.ifPresent(session -> styleSpot.failAnalysis(preparation.analysisAttempt())));
	}

	@Transactional(readOnly = true)
	public StyleResultResponse getResult(String spotCode) {
		StyleSpot styleSpot = styleSpotRepository.findById(normalize(spotCode))
				.orElseThrow(() -> new BusinessException(ErrorCode.STYLE_SPOT_NOT_FOUND));
		if (styleSpot.getStatus() != StyleSpotStatus.RESULT) {
			throw new BusinessException(ErrorCode.STYLE_RESULT_NOT_FOUND);
		}
		StyleSpotSession activeSession = findActiveSession(styleSpot);
		return findBySession(activeSession.getPassportSession().getId());
	}

	private StyleResultResponse findBySession(Long passportSessionId) {
		StyleResult styleResult = styleResultRepository
				.findByPassportSessionId(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.STYLE_RESULT_NOT_FOUND));
		return styleResultResponseMapper.from(styleResult);
	}

	private StyleSpotSession findActiveSession(StyleSpot styleSpot) {
		return styleSpotSessionRepository.findActiveByStyleSpotCode(styleSpot.getCode())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_SESSION_STATUS));
	}

	private StyleSpot findForUpdate(String spotCode) {
		return styleSpotRepository.findByCodeForUpdate(normalize(spotCode))
				.orElseThrow(() -> new BusinessException(ErrorCode.STYLE_SPOT_NOT_FOUND));
	}

	private String normalize(String spotCode) {
		return spotCode.trim().toUpperCase(Locale.ROOT);
	}
}
