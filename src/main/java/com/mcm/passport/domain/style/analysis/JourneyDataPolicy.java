package com.mcm.passport.domain.style.analysis;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class JourneyDataPolicy {

	public void validateForAnalysis(JourneyDataSnapshot journeyData) {
		if (journeyData.responses().isEmpty()
				|| journeyData.stamps().isEmpty()
				|| journeyData.taggedProducts().isEmpty()) {
			throw new BusinessException(ErrorCode.JOURNEY_NOT_COMPLETED);
		}
	}
}
