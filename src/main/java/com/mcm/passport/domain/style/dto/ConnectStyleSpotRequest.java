package com.mcm.passport.domain.style.dto;

import jakarta.validation.constraints.NotNull;

public record ConnectStyleSpotRequest(
		@NotNull Long passportSessionId
) {
}
