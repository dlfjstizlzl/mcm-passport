package com.mcm.passport.domain.passport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PassportSessionCreateRequest(
		@NotBlank(message = "cardUid는 필수입니다.")
		@Size(max = 100, message = "cardUid는 100자 이하여야 합니다.")
		String cardUid
) {
}
