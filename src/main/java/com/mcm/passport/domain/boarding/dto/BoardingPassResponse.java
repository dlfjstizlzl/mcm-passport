package com.mcm.passport.domain.boarding.dto;

import com.mcm.passport.domain.boarding.entity.BoardingPass;
import java.time.Instant;

public record BoardingPassResponse(Long boardingPassId, String gate, Instant issuedAt) {
	public static BoardingPassResponse from(BoardingPass boardingPass) {
		return new BoardingPassResponse(boardingPass.getId(), boardingPass.getGate(), boardingPass.getIssuedAt());
	}
}
