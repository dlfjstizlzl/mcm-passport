package com.mcm.passport.domain.journey.repository;

import com.mcm.passport.domain.journey.entity.JourneySpot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JourneySpotRepository extends JpaRepository<JourneySpot, Long> {

	List<JourneySpot> findAllByActiveTrueOrderBySequenceAsc();

	Optional<JourneySpot> findByIdAndActiveTrue(Long id);

	Optional<JourneySpot> findByCode(String code);
}
