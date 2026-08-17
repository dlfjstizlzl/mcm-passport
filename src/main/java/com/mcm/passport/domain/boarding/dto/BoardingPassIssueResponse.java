package com.mcm.passport.domain.boarding.dto;

import com.mcm.passport.domain.boarding.entity.BoardingPass;

import java.time.Instant;

public record BoardingPassIssueResponse(
		Long boardingPassId,
		Long passportSessionId,
		String gate,
		Instant issuedAt
) {
	public static BoardingPassIssueResponse from(BoardingPass boardingPass) {
		return new BoardingPassIssueResponse(
				boardingPass.getId(),
				boardingPass.getPassportSession().getId(),
				boardingPass.getGate(),
				boardingPass.getIssuedAt()
		);
	}
}
