package com.mcm.passport.domain.product.dto;

import jakarta.validation.constraints.NotNull;

public record ProductTagCreateRequest(
		@NotNull(message = "productId는 필수입니다.")
		Long productId
) {
}
