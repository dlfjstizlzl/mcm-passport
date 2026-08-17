package com.mcm.passport.domain.product.dto;

import com.mcm.passport.domain.product.entity.Product;

public record ProductResponse(
		Long id,
		String name,
		String category,
		String color,
		String material,
		String silhouette,
		String imageUrl,
		boolean recommendable
) {

	public static ProductResponse from(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getName(),
				product.getCategory(),
				product.getColor(),
				product.getMaterial(),
				product.getSilhouette(),
				product.getImageUrl(),
				product.isRecommendable()
		);
	}
}
