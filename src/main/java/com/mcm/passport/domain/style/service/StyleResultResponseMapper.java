package com.mcm.passport.domain.style.service;

import com.mcm.passport.domain.product.entity.Product;
import com.mcm.passport.domain.product.repository.ProductRepository;
import com.mcm.passport.domain.style.dto.StyleResultResponse;
import com.mcm.passport.domain.style.entity.StyleResult;
import org.springframework.stereotype.Component;

@Component
public class StyleResultResponseMapper {

	private final ProductRepository productRepository;

	public StyleResultResponseMapper(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public StyleResultResponse from(StyleResult styleResult) {
		String productName = styleResult.getRecommendedProduct().getDisplayName();
		String imageUrl = productRepository.findFirstByNameAndActiveTrue(productName)
				.map(Product::getImageUrl)
				.orElse(null);
		return StyleResultResponse.from(styleResult, imageUrl);
	}
}
