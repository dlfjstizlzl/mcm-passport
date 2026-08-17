package com.mcm.passport.domain.passport.dto;

import com.mcm.passport.domain.passport.entity.PassportSession;

import java.time.Instant;

public record PassportSessionCreateResponse(
		Long passportSessionId,
		String status,
		Instant startedAt
) {
	public static PassportSessionCreateResponse from(PassportSession session) {
		return new PassportSessionCreateResponse(
				session.getId(),
				session.getStatus().name(),
				session.getStartedAt()
		);
	}
}
