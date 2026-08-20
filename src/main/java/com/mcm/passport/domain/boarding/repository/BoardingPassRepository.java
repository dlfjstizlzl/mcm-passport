package com.mcm.passport.domain.boarding.repository;

import com.mcm.passport.domain.boarding.entity.BoardingPass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BoardingPassRepository extends JpaRepository<BoardingPass, Long> {
	Optional<BoardingPass> findByPassportSession_Id(Long passportSessionId);
}
