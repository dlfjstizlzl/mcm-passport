package com.mcm.passport.domain.style.repository;

import com.mcm.passport.domain.style.entity.StyleSpot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StyleSpotRepository extends JpaRepository<StyleSpot, String> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select spot from StyleSpot spot where spot.code = :code")
	Optional<StyleSpot> findByCodeForUpdate(@Param("code") String code);
}
