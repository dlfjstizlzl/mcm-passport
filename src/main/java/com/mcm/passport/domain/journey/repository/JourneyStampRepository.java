package com.mcm.passport.domain.journey.repository;

import com.mcm.passport.domain.journey.entity.JourneyStamp;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JourneyStampRepository extends JpaRepository<JourneyStamp, Long> {

	@EntityGraph(attributePaths = "journeySpot")
	List<JourneyStamp> findAllByPassportSession_IdOrderByJourneySpot_SequenceAscIdAsc(Long passportSessionId);

	@EntityGraph(attributePaths = "journeySpot")
	List<JourneyStamp> findAllByPassportSession_IdOrderByIdAsc(Long passportSessionId);

	boolean existsByPassportSession_IdAndJourneySpot_Id(Long passportSessionId, Long journeySpotId);
	long countByPassportSession_IdAndJourneySpot_IdIn(Long passportSessionId, List<Long> journeySpotIds);
}
