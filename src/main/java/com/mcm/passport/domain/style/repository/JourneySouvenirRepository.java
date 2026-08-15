package com.mcm.passport.domain.style.repository;

import com.mcm.passport.domain.style.entity.JourneySouvenir;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JourneySouvenirRepository extends JpaRepository<JourneySouvenir, Long> {

	@EntityGraph(attributePaths = {"passportSession", "styleResult"})
	Optional<JourneySouvenir> findByPassportSessionId(Long passportSessionId);
}
