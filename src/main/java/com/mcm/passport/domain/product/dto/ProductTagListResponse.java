package com.mcm.passport.domain.product.dto;

import com.mcm.passport.domain.product.entity.ProductTag;

import java.time.Instant;

public record ProductTagListResponse(
		Long productId,
		String name,
		Instant taggedAt
) {
	public static ProductTagListResponse from(ProductTag tag) {
		return new ProductTagListResponse(tag.getProduct().getId(), tag.getProduct().getName(), tag.getTaggedAt());
	}
}
