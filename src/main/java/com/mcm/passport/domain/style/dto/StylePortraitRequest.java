package com.mcm.passport.domain.style.dto;

import jakarta.validation.constraints.NotBlank;

public record StylePortraitRequest(
		@NotBlank String imageUrl,
		boolean consent
) {
}
