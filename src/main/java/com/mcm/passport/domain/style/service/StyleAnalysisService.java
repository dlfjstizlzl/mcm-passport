package com.mcm.passport.domain.style.service;

import com.mcm.passport.domain.journey.service.JourneyDataReader;
import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.style.analysis.JourneyDataPolicy;
import com.mcm.passport.domain.style.analysis.StyleAnalysisDecision;
import com.mcm.passport.domain.style.analysis.StyleAnalysisEngine;
import com.mcm.passport.domain.style.dto.StyleResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StyleAnalysisService {

	private static final Logger log = LoggerFactory.getLogger(StyleAnalysisService.class);

	private final StyleAnalysisTransactionService transactionService;
	private final JourneyDataReader journeyDataReader;
	private final JourneyDataPolicy journeyDataPolicy;
	private final StyleAnalysisEngine analysisEngine;

	public StyleAnalysisService(
			StyleAnalysisTransactionService transactionService,
			JourneyDataReader journeyDataReader,
			JourneyDataPolicy journeyDataPolicy,
			StyleAnalysisEngine analysisEngine
	) {
		this.transactionService = transactionService;
		this.journeyDataReader = journeyDataReader;
		this.journeyDataPolicy = journeyDataPolicy;
		this.analysisEngine = analysisEngine;
	}

	public StyleResultResponse analyze(String spotCode) {
		StyleAnalysisPreparation preparation = transactionService.prepare(spotCode);
		if (preparation.alreadyCompleted()) {
			return preparation.existingResult();
		}

		try {
			JourneyDataSnapshot journeyData = journeyDataReader.read(preparation.passportSessionId());
			journeyDataPolicy.validateForAnalysis(journeyData);
			StyleAnalysisDecision decision = analysisEngine.analyze(journeyData);
			return transactionService.complete(preparation, decision);
		} catch (RuntimeException exception) {
			try {
				transactionService.fail(preparation);
			} catch (RuntimeException recoveryException) {
				log.error("Failed to restore Style Spot after analysis failure", recoveryException);
			}
			throw exception;
		}
	}

	public StyleResultResponse getResult(String spotCode) {
		return transactionService.getResult(spotCode);
	}
}
