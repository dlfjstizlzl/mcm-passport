package com.mcm.passport.domain.style.service;

import com.mcm.passport.domain.style.dto.StyleResultResponse;
import com.mcm.passport.domain.style.dto.StyleSpotConnectResponse;
import com.mcm.passport.domain.style.dto.StyleSpotDisplayResponse;
import com.mcm.passport.domain.style.dto.StyleSpotResponse;
import com.mcm.passport.domain.style.dto.StyleSpotSessionResponse;
import com.mcm.passport.domain.style.entity.StyleSpotStatus;
import org.springframework.stereotype.Service;

@Service
public class StyleSpotFlowService {

	private final StyleSpotService styleSpotService;
	private final StyleAnalysisService styleAnalysisService;

	public StyleSpotFlowService(
			StyleSpotService styleSpotService,
			StyleAnalysisService styleAnalysisService
	) {
		this.styleSpotService = styleSpotService;
		this.styleAnalysisService = styleAnalysisService;
	}

	public StyleSpotConnectResponse connectAndAnalyze(String styleSpotId, Long passportSessionId) {
		StyleSpotSessionResponse connection = styleSpotService.connectSession(styleSpotId, passportSessionId);
		StyleResultResponse result = styleAnalysisService.analyze(styleSpotId);
		return new StyleSpotConnectResponse(
				connection.id(),
				connection.styleSpotId(),
				connection.passportSessionId(),
				StyleSpotStatus.RESULT,
				result
		);
	}

	public StyleSpotDisplayResponse display(String styleSpotId) {
		StyleSpotResponse spot = styleSpotService.display(styleSpotId);
		StyleResultResponse result = spot.status() == StyleSpotStatus.RESULT
				? styleAnalysisService.getResult(styleSpotId)
				: null;
		return new StyleSpotDisplayResponse(
				spot.spotCode(),
				spot.styleSpotSessionId(),
				spot.status(),
				result
		);
	}
}
