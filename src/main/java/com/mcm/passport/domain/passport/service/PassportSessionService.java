package com.mcm.passport.domain.passport.service;

import com.mcm.passport.domain.passport.dto.PassportSessionCreateRequest;
import com.mcm.passport.domain.passport.dto.PassportSessionCreateResponse;
import com.mcm.passport.domain.passport.dto.PassportSessionResponse;
import com.mcm.passport.domain.passport.entity.PassportCard;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.entity.PassportSessionStatus;
import com.mcm.passport.domain.passport.repository.PassportCardRepository;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PassportSessionService {

	private static final String VIRTUAL_CARD_PREFIX = "MCM-WEB-";

	private final PassportCardRepository passportCardRepository;
	private final PassportSessionRepository passportSessionRepository;

	@Value("${mcm.passport.allow-virtual-cards:false}")
	private boolean allowVirtualCards;

	public PassportSessionService(
			PassportCardRepository passportCardRepository,
			PassportSessionRepository passportSessionRepository
	) {
		this.passportCardRepository = passportCardRepository;
		this.passportSessionRepository = passportSessionRepository;
	}

	@Transactional
	public PassportSessionCreateResponse create(PassportSessionCreateRequest request) {
		String cardUid = request.cardUid().trim();
		PassportCard passportCard = passportCardRepository.findByCardUidForUpdate(cardUid)
				.orElseGet(() -> issueVirtualCard(cardUid));

		if (!passportCard.isActive()) {
			throw new BusinessException(ErrorCode.PASSPORT_CARD_INACTIVE);
		}
		if (passportSessionRepository.existsByPassportCard_IdAndStatusNot(
				passportCard.getId(),
				PassportSessionStatus.COMPLETED
		)) {
			throw new BusinessException(ErrorCode.ACTIVE_PASSPORT_SESSION_EXISTS);
		}

		PassportSession passportSession = passportSessionRepository.save(PassportSession.start(passportCard));
		return PassportSessionCreateResponse.from(passportSession);
	}

	private PassportCard issueVirtualCard(String cardUid) {
		if (!allowVirtualCards || !cardUid.startsWith(VIRTUAL_CARD_PREFIX)) {
			throw new BusinessException(ErrorCode.PASSPORT_CARD_NOT_FOUND);
		}
		return passportCardRepository.save(PassportCard.issue(cardUid));
	}

	@Transactional(readOnly = true)
	public PassportSessionResponse get(Long passportSessionId) {
		PassportSession passportSession = passportSessionRepository.findById(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND));
		return PassportSessionResponse.from(passportSession);
	}
}
