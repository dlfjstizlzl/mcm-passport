package com.mcm.passport.domain.passport.dto;

import com.mcm.passport.domain.passport.entity.PassportSession;

import java.time.Instant;

public record PassportSessionResponse(
		Long passportSessionId,
		String status,
		Instant startedAt,
		Instant completedAt
) {

	public static PassportSessionResponse from(PassportSession passportSession) {
		return new PassportSessionResponse(
				passportSession.getId(),
				passportSession.getStatus().name(),
				passportSession.getStartedAt(),
				passportSession.getCompletedAt()
		);
	}
}
