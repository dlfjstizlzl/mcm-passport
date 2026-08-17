package com.mcm.passport.domain.product.repository;

import com.mcm.passport.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

	Optional<Product> findByIdAndActiveTrue(Long id);

	Optional<Product> findFirstByName(String name);

	List<Product> findAllByActiveTrueOrderByIdAsc();
}
