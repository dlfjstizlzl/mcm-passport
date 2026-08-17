package com.mcm.passport.domain.style.repository;

import com.mcm.passport.domain.style.entity.StylePortrait;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StylePortraitRepository extends JpaRepository<StylePortrait, Long> {

	@EntityGraph(attributePaths = "passportSession")
	Optional<StylePortrait> findByPassportSessionId(Long passportSessionId);
}
