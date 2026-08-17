package com.mcm.passport.domain.product.dto;

import com.mcm.passport.domain.product.entity.ProductTag;

import java.time.Instant;

public record ProductTagResponse(
		Long productTagId,
		ProductResponse product,
		Instant taggedAt
) {

	public static ProductTagResponse from(ProductTag productTag) {
		return new ProductTagResponse(
				productTag.getId(),
				ProductResponse.from(productTag.getProduct()),
				productTag.getTaggedAt()
		);
	}
}
