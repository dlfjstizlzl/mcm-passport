package com.mcm.passport.domain.product.service;

import com.mcm.passport.domain.product.dto.ProductResponse;
import com.mcm.passport.domain.product.entity.Product;
import com.mcm.passport.domain.product.repository.ProductRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public ProductResponse get(Long productId) {
		Product product = productRepository.findByIdAndActiveTrue(productId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
		return ProductResponse.from(product);
	}
}
