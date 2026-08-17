package com.mcm.passport.domain.product.controller;

import com.mcm.passport.domain.product.dto.ProductResponse;
import com.mcm.passport.domain.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping("/{productId}")
	public ResponseEntity<ProductResponse> get(@PathVariable Long productId) {
		return ResponseEntity.ok(productService.get(productId));
	}
}
