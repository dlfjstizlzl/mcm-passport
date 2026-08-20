package com.mcm.passport.domain.product.repository;

import com.mcm.passport.domain.product.entity.ProductTag;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {

	@EntityGraph(attributePaths = "product")
	List<ProductTag> findAllByPassportSession_IdOrderByIdAsc(Long passportSessionId);

	Optional<ProductTag> findByPassportSession_IdAndProduct_Id(Long passportSessionId, Long productId);
}
