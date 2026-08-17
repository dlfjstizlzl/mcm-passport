package com.mcm.passport.domain.boarding.service;

import com.mcm.passport.domain.boarding.dto.BoardingPassResponse;
import com.mcm.passport.domain.boarding.entity.BoardingPass;
import com.mcm.passport.domain.boarding.repository.BoardingPassRepository;
import com.mcm.passport.domain.journey.entity.JourneySpot;
import com.mcm.passport.domain.journey.repository.JourneySpotRepository;
import com.mcm.passport.domain.journey.repository.JourneyStampRepository;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.entity.PassportSessionStatus;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class BoardingPassService {
	private static final String DEFAULT_GATE = "STYLE_SPOT";
	private final PassportSessionRepository passportSessionRepository;
	private final JourneySpotRepository journeySpotRepository;
	private final JourneyStampRepository journeyStampRepository;
	private final BoardingPassRepository boardingPassRepository;

	public BoardingPassService(PassportSessionRepository passportSessionRepository,
			JourneySpotRepository journeySpotRepository, JourneyStampRepository journeyStampRepository,
			BoardingPassRepository boardingPassRepository) {
		this.passportSessionRepository = passportSessionRepository;
		this.journeySpotRepository = journeySpotRepository;
		this.journeyStampRepository = journeyStampRepository;
		this.boardingPassRepository = boardingPassRepository;
	}

	@Transactional
	public BoardingPassResponse issue(Long passportSessionId) {
		PassportSession session = passportSessionRepository.findByIdForUpdate(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND));
		if (session.getStatus() != PassportSessionStatus.EXPLORING) {
			throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
		}
		List<Long> requiredSpotIds = journeySpotRepository.findAllByRequiredTrueAndActiveTrueOrderBySequenceAsc()
				.stream().map(JourneySpot::getId).toList();
		long stampedCount = requiredSpotIds.isEmpty() ? 0
				: journeyStampRepository.countByPassportSession_IdAndJourneySpot_IdIn(passportSessionId, requiredSpotIds);
		if (stampedCount != requiredSpotIds.size()) throw new BusinessException(ErrorCode.JOURNEY_NOT_COMPLETED);
		if (boardingPassRepository.existsByPassportSession_Id(passportSessionId)) {
			throw new BusinessException(ErrorCode.BOARDING_PASS_ALREADY_EXISTS);
		}
		BoardingPass boardingPass = boardingPassRepository.save(BoardingPass.issue(session, DEFAULT_GATE));
		session.markReadyToBoard();
		return BoardingPassResponse.from(boardingPass);
	}

	@Transactional(readOnly = true)
	public BoardingPassResponse get(Long passportSessionId) {
		if (!passportSessionRepository.existsById(passportSessionId)) {
			throw new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND);
		}
		return boardingPassRepository.findByPassportSession_Id(passportSessionId)
				.map(BoardingPassResponse::from)
				.orElseThrow(() -> new BusinessException(ErrorCode.BOARDING_PASS_NOT_FOUND));
	}
}
