package com.mcm.passport.domain.journey.repository;

import com.mcm.passport.domain.journey.entity.GuideQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuideQuestionRepository extends JpaRepository<GuideQuestion, Long> {

	List<GuideQuestion> findAllByJourneySpot_IdAndActiveTrueOrderBySequenceAsc(Long journeySpotId);

	Optional<GuideQuestion> findByJourneySpot_IdAndCode(Long journeySpotId, String code);
}
