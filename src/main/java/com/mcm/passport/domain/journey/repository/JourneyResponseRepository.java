package com.mcm.passport.domain.journey.repository;

import com.mcm.passport.domain.journey.entity.JourneyResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JourneyResponseRepository extends JpaRepository<JourneyResponse, Long> {

	List<JourneyResponse> findAllByPassportSession_IdOrderByIdAsc(Long passportSessionId);
}
