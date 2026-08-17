package com.mcm.passport.domain.product.service;

import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.entity.PassportSessionStatus;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.product.dto.ProductTagCreateRequest;
import com.mcm.passport.domain.product.dto.ProductTagResponse;
import com.mcm.passport.domain.product.entity.Product;
import com.mcm.passport.domain.product.entity.ProductTag;
import com.mcm.passport.domain.product.repository.ProductRepository;
import com.mcm.passport.domain.product.repository.ProductTagRepository;
import com.mcm.passport.global.exception.BusinessException;
import com.mcm.passport.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductTagService {

	private final PassportSessionRepository passportSessionRepository;
	private final ProductRepository productRepository;
	private final ProductTagRepository productTagRepository;

	public ProductTagService(
			PassportSessionRepository passportSessionRepository,
			ProductRepository productRepository,
			ProductTagRepository productTagRepository
	) {
		this.passportSessionRepository = passportSessionRepository;
		this.productRepository = productRepository;
		this.productTagRepository = productTagRepository;
	}

	@Transactional
	public ProductTagResponse create(Long passportSessionId, ProductTagCreateRequest request) {
		PassportSession passportSession = passportSessionRepository.findByIdForUpdate(passportSessionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND));
		if (passportSession.getStatus() != PassportSessionStatus.EXPLORING) {
			throw new BusinessException(ErrorCode.INVALID_SESSION_STATUS);
		}

		Product product = productRepository.findByIdAndActiveTrue(request.productId())
				.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
		if (productTagRepository.existsByPassportSession_IdAndProduct_Id(passportSessionId, product.getId())) {
			throw new BusinessException(ErrorCode.PRODUCT_ALREADY_TAGGED);
		}

		ProductTag productTag = productTagRepository.save(ProductTag.create(passportSession, product));
		return ProductTagResponse.from(productTag);
	}

	@Transactional(readOnly = true)
	public List<ProductTagResponse> getAll(Long passportSessionId) {
		if (!passportSessionRepository.existsById(passportSessionId)) {
			throw new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND);
		}
		return productTagRepository.findAllByPassportSession_IdOrderByIdAsc(passportSessionId)
				.stream()
				.map(ProductTagResponse::from)
				.toList();
	}
}
