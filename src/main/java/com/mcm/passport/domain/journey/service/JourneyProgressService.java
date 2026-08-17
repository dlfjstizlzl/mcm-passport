package com.mcm.passport.domain.journey.service;

import com.mcm.passport.domain.journey.dto.JourneyProgressResponse;
import com.mcm.passport.domain.journey.dto.JourneyProgressSpotResponse;
import com.mcm.passport.domain.journey.entity.JourneyStamp;
import com.mcm.passport.domain.journey.repository.JourneySpotRepository;
import com.mcm.passport.domain.journey.repository.JourneyStampRepository;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JourneyProgressService {
	private final PassportSessionRepository passportSessionRepository;
	private final JourneySpotRepository journeySpotRepository;
	private final JourneyStampRepository journeyStampRepository;

	public JourneyProgressService(PassportSessionRepository passportSessionRepository,
			JourneySpotRepository journeySpotRepository, JourneyStampRepository journeyStampRepository) {
		this.passportSessionRepository = passportSessionRepository;
		this.journeySpotRepository = journeySpotRepository;
		this.journeyStampRepository = journeyStampRepository;
	}

	@Transactional(readOnly = true)
	public JourneyProgressResponse get(Long passportSessionId) {
		PassportSession session = passportSessionRepository.findById(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND));
		Map<Long, Instant> stampedAtBySpotId = journeyStampRepository
				.findAllByPassportSession_IdOrderByIdAsc(passportSessionId)
				.stream()
				.collect(Collectors.toMap(
						stamp -> stamp.getJourneySpot().getId(),
						JourneyStamp::getStampedAt,
						(first, ignored) -> first
				));
		var spots = journeySpotRepository.findAllByActiveTrueOrderBySequenceAsc().stream()
				.map(spot -> JourneyProgressSpotResponse.from(spot, stampedAtBySpotId.get(spot.getId())))
				.toList();
		return JourneyProgressResponse.of(session, spots);
	}
}
