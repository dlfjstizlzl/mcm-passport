package com.mcm.passport.domain.boarding.dto;

import com.mcm.passport.domain.boarding.entity.BoardingPass;
import java.time.Instant;

public record BoardingPassResponse(Long boardingPassId, Long passportSessionId, String gate, Instant issuedAt) {
	public static BoardingPassResponse from(BoardingPass boardingPass) {
		return new BoardingPassResponse(boardingPass.getId(), boardingPass.getPassportSession().getId(),
				boardingPass.getGate(), boardingPass.getIssuedAt());
	}
}
