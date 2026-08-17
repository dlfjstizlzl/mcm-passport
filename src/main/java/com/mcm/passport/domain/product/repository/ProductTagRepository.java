package com.mcm.passport.domain.product.repository;

import com.mcm.passport.domain.product.entity.ProductTag;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {

	@EntityGraph(attributePaths = "product")
	List<ProductTag> findAllByPassportSession_IdOrderByIdAsc(Long passportSessionId);

	boolean existsByPassportSession_IdAndProduct_Id(Long passportSessionId, Long productId);
}
