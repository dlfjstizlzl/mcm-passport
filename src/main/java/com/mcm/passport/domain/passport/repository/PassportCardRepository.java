package com.mcm.passport.domain.passport.repository;

import com.mcm.passport.domain.passport.entity.PassportCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PassportCardRepository extends JpaRepository<PassportCard, Long> {

	Optional<PassportCard> findByCardUid(String cardUid);
}
