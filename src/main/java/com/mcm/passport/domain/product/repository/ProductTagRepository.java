package com.mcm.passport.domain.product.repository;

import com.mcm.passport.domain.product.entity.ProductTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {

	List<ProductTag> findAllByPassportSession_IdOrderByIdAsc(Long passportSessionId);
}
