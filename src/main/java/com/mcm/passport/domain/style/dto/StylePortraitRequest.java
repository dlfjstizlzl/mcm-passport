package com.mcm.passport.domain.style.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StylePortraitRequest(
		@NotBlank @Size(max = 1000) String imageUrl,
		@NotNull @AssertTrue Boolean consent
) {
}
