package com.mcm.passport.domain.style.service;

import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.style.dto.StylePortraitRequest;
import com.mcm.passport.domain.style.dto.StylePortraitResponse;
import com.mcm.passport.domain.style.entity.StylePortrait;
import com.mcm.passport.domain.style.repository.StylePortraitRepository;
import com.mcm.passport.domain.style.repository.StyleResultRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StylePortraitService {

	private final PassportSessionRepository passportSessionRepository;
	private final StyleResultRepository styleResultRepository;
	private final StylePortraitRepository stylePortraitRepository;

	public StylePortraitService(
			PassportSessionRepository passportSessionRepository,
			StyleResultRepository styleResultRepository,
			StylePortraitRepository stylePortraitRepository
	) {
		this.passportSessionRepository = passportSessionRepository;
		this.styleResultRepository = styleResultRepository;
		this.stylePortraitRepository = stylePortraitRepository;
	}

	@Transactional
	public StylePortraitResponse create(Long passportSessionId, StylePortraitRequest request) {
		PassportSession passportSession = passportSessionRepository.findByIdForUpdate(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND));
		if (!styleResultRepository.findByPassportSessionId(passportSessionId).isPresent()) {
			throw new BusinessException(ErrorCode.STYLE_RESULT_NOT_FOUND);
		}
		if (stylePortraitRepository.findByPassportSessionId(passportSessionId).isPresent()) {
			throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
		}
		StylePortrait portrait = stylePortraitRepository.save(
				StylePortrait.create(passportSession, request.imageUrl(), request.consent())
		);
		return StylePortraitResponse.from(portrait);
	}

	@Transactional(readOnly = true)
	public StylePortraitResponse get(Long passportSessionId) {
		return StylePortraitResponse.from(find(passportSessionId));
	}

	@Transactional
	public void delete(Long passportSessionId) {
		stylePortraitRepository.delete(find(passportSessionId));
	}

	private StylePortrait find(Long passportSessionId) {
		return stylePortraitRepository.findByPassportSessionId(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.STYLE_PORTRAIT_NOT_FOUND));
	}
}
