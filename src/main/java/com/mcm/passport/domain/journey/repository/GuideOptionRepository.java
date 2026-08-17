package com.mcm.passport.domain.journey.repository;

import com.mcm.passport.domain.journey.entity.GuideOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuideOptionRepository extends JpaRepository<GuideOption, Long> {

	List<GuideOption> findAllByGuideQuestion_IdAndActiveTrueOrderBySequenceAsc(Long guideQuestionId);

	Optional<GuideOption> findByGuideQuestion_IdAndCode(Long guideQuestionId, String code);
}
