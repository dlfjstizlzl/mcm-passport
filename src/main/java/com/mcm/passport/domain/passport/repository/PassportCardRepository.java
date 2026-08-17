package com.mcm.passport.domain.passport.repository;

import com.mcm.passport.domain.passport.entity.PassportCard;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PassportCardRepository extends JpaRepository<PassportCard, Long> {

	Optional<PassportCard> findByCardUid(String cardUid);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select card from PassportCard card where card.cardUid = :cardUid")
	Optional<PassportCard> findByCardUidForUpdate(@Param("cardUid") String cardUid);
}
