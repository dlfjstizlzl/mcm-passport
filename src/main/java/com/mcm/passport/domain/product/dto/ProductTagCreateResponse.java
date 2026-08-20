package com.mcm.passport.domain.product.dto;

import com.mcm.passport.domain.product.entity.Product;
import com.mcm.passport.domain.product.entity.ProductTag;

import java.time.Instant;

public record ProductTagCreateResponse(
		Long productTagId,
		TaggedProductResponse product,
		Instant taggedAt
) {
	public static ProductTagCreateResponse from(ProductTag tag) {
		return new ProductTagCreateResponse(
				tag.getId(),
				TaggedProductResponse.from(tag.getProduct()),
				tag.getTaggedAt()
		);
	}

	public record TaggedProductResponse(
			Long id,
			String name,
			String category,
			String description,
			String color,
			String material,
			String silhouette,
			String imageUrl
	) {
		public static TaggedProductResponse from(Product product) {
			return new TaggedProductResponse(
					product.getId(), product.getName(), product.getCategory(), product.getDescription(),
					product.getColor(), product.getMaterial(), product.getSilhouette(), product.getImageUrl()
			);
		}
	}
}
