package com.mcm.passport.domain.style.service;

import com.mcm.passport.domain.style.dto.StyleResultResponse;
import com.mcm.passport.domain.style.entity.StyleResult;
import com.mcm.passport.domain.style.repository.StyleResultRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PassportStyleResultService {

	private final StyleResultRepository styleResultRepository;

	public PassportStyleResultService(StyleResultRepository styleResultRepository) {
		this.styleResultRepository = styleResultRepository;
	}

	@Transactional(readOnly = true)
	public StyleResultResponse get(Long passportSessionId) {
		StyleResult result = styleResultRepository.findByPassportSessionId(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.STYLE_RESULT_NOT_FOUND));
		return StyleResultResponse.from(result);
	}
}
