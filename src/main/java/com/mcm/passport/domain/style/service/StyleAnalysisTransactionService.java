package com.mcm.passport.domain.style.service;

import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.style.analysis.StyleAnalysisDecision;
import com.mcm.passport.domain.style.dto.StyleResultResponse;
import com.mcm.passport.domain.style.entity.StyleResult;
import com.mcm.passport.domain.style.entity.StyleSpot;
import com.mcm.passport.domain.style.entity.StyleSpotStatus;
import com.mcm.passport.domain.style.repository.StyleResultRepository;
import com.mcm.passport.domain.style.repository.StyleSpotRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class StyleAnalysisTransactionService {

	private final StyleSpotRepository styleSpotRepository;
	private final StyleResultRepository styleResultRepository;

	public StyleAnalysisTransactionService(
			StyleSpotRepository styleSpotRepository,
			StyleResultRepository styleResultRepository
	) {
		this.styleSpotRepository = styleSpotRepository;
		this.styleResultRepository = styleResultRepository;
	}

	@Transactional
	public StyleAnalysisPreparation prepare(String spotCode) {
		StyleSpot styleSpot = findForUpdate(spotCode);
		if (styleSpot.getStatus() == StyleSpotStatus.RESULT) {
			StyleResultResponse existing = findByCurrentSession(styleSpot);
			return new StyleAnalysisPreparation(
					styleSpot.getCode(),
					styleSpot.getPassportSession().getId(),
					styleSpot.getAnalysisAttempt(),
					existing
			);
		}

		long analysisAttempt = styleSpot.startAnalysis();
		return new StyleAnalysisPreparation(
				styleSpot.getCode(),
				styleSpot.getPassportSession().getId(),
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
		if (!styleSpot.isCurrentAnalysisAttempt(
				preparation.passportSessionId(),
				preparation.analysisAttempt()
		)) {
			throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
		}
		if (styleSpot.getStatus() == StyleSpotStatus.RESULT) {
			return findByCurrentSession(styleSpot);
		}
		if (styleSpot.getStatus() != StyleSpotStatus.ANALYZING) {
			throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
		}

		PassportSession passportSession = styleSpot.getPassportSession();
		StyleResult styleResult = StyleResult.create(
				passportSession,
				decision.analysis(),
				decision.usedFallback()
		);
		styleResultRepository.save(styleResult);
		styleSpot.finishAnalysis(preparation.passportSessionId(), preparation.analysisAttempt());

		return StyleResultResponse.from(styleResult);
	}

	@Transactional
	public void fail(StyleAnalysisPreparation preparation) {
		styleSpotRepository.findByCodeForUpdate(preparation.spotCode())
				.ifPresent(styleSpot -> styleSpot.failAnalysis(
						preparation.passportSessionId(),
						preparation.analysisAttempt()
				));
	}

	@Transactional(readOnly = true)
	public StyleResultResponse getResult(String spotCode) {
		StyleSpot styleSpot = styleSpotRepository.findById(normalize(spotCode))
				.orElseThrow(() -> new BusinessException(ErrorCode.STYLE_SPOT_NOT_FOUND));
		if (styleSpot.getStatus() != StyleSpotStatus.RESULT || styleSpot.getPassportSession() == null) {
			throw new BusinessException(ErrorCode.STYLE_RESULT_NOT_FOUND);
		}
		return findByCurrentSession(styleSpot);
	}

	private StyleResultResponse findByCurrentSession(StyleSpot styleSpot) {
		StyleResult styleResult = styleResultRepository
				.findByPassportSessionId(styleSpot.getPassportSession().getId())
				.orElseThrow(() -> new BusinessException(ErrorCode.STYLE_RESULT_NOT_FOUND));
		return StyleResultResponse.from(styleResult);
	}

	private StyleSpot findForUpdate(String spotCode) {
		return styleSpotRepository.findByCodeForUpdate(normalize(spotCode))
				.orElseThrow(() -> new BusinessException(ErrorCode.STYLE_SPOT_NOT_FOUND));
	}

	private String normalize(String spotCode) {
		return spotCode.trim().toUpperCase(Locale.ROOT);
	}
}
