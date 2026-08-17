package com.mcm.passport.domain.journey.repository;

import com.mcm.passport.domain.journey.entity.GuideResponse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GuideResponseRepository extends JpaRepository<GuideResponse, Long> {

	Optional<GuideResponse> findByPassportSession_IdAndGuideQuestion_Id(
			Long passportSessionId,
			Long guideQuestionId
	);

	long countByPassportSession_IdAndGuideQuestion_IdIn(
			Long passportSessionId,
			Collection<Long> guideQuestionIds
	);

	@EntityGraph(attributePaths = {"guideQuestion", "guideQuestion.journeySpot", "guideOption"})
	@Query("""
			select response
			from GuideResponse response
			join response.guideQuestion question
			join question.journeySpot spot
			where response.passportSession.id = :passportSessionId
			order by spot.sequence asc, question.sequence asc, response.id asc
			""")
	List<GuideResponse> findAllForStyleAnalysis(@Param("passportSessionId") Long passportSessionId);
}
