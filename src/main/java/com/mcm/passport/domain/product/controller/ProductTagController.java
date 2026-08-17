package com.mcm.passport.domain.product.controller;

import com.mcm.passport.domain.product.dto.ProductTagCreateRequest;
import com.mcm.passport.domain.product.dto.ProductTagCreateResponse;
import com.mcm.passport.domain.product.dto.ProductTagListResponse;
import com.mcm.passport.domain.product.service.ProductTagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/passport-sessions/{passportSessionId}/product-tags")
public class ProductTagController {

	private final ProductTagService productTagService;

	public ProductTagController(ProductTagService productTagService) {
		this.productTagService = productTagService;
	}

	@PostMapping
	public ResponseEntity<ProductTagCreateResponse> create(
			@PathVariable Long passportSessionId,
			@Valid @RequestBody ProductTagCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(productTagService.create(passportSessionId, request));
	}

	@GetMapping
	public ResponseEntity<List<ProductTagListResponse>> getAll(@PathVariable Long passportSessionId) {
		return ResponseEntity.ok(productTagService.getAll(passportSessionId));
	}
}
