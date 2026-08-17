package com.mcm.passport.domain.style.repository;

import com.mcm.passport.domain.style.entity.StyleResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StyleResultRepository extends JpaRepository<StyleResult, Long> {

	@EntityGraph(attributePaths = "passportSession")
	Optional<StyleResult> findByPassportSessionId(Long passportSessionId);
}
