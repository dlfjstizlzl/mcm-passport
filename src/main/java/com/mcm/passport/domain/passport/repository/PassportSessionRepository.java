package com.mcm.passport.domain.passport.repository;

import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.entity.PassportSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PassportSessionRepository extends JpaRepository<PassportSession, Long> {

	boolean existsByPassportCard_IdAndStatusNot(Long passportCardId, PassportSessionStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select session from PassportSession session where session.id = :id")
	Optional<PassportSession> findByIdForUpdate(@Param("id") Long id);
}
