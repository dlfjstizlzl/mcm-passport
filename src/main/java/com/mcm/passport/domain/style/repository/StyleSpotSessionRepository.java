package com.mcm.passport.domain.style.repository;

import com.mcm.passport.domain.style.entity.StyleSpotSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StyleSpotSessionRepository extends JpaRepository<StyleSpotSession, Long> {

	@EntityGraph(attributePaths = {"styleSpot", "passportSession"})
	@Query("""
			select connection
			from StyleSpotSession connection
			where connection.styleSpot.code = :styleSpotCode
			  and connection.disconnectedAt is null
			""")
	Optional<StyleSpotSession> findActiveByStyleSpotCode(@Param("styleSpotCode") String styleSpotCode);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"styleSpot", "passportSession"})
	@Query("select connection from StyleSpotSession connection where connection.id = :id")
	Optional<StyleSpotSession> findByIdForUpdate(@Param("id") Long id);
}
