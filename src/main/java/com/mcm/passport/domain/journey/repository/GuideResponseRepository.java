package com.mcm.passport.domain.journey.repository;

import com.mcm.passport.domain.journey.entity.GuideResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuideResponseRepository extends JpaRepository<GuideResponse, Long> {

	Optional<GuideResponse> findByPassportSession_IdAndGuideQuestion_Id(
			Long passportSessionId,
			Long guideQuestionId
	);
}
