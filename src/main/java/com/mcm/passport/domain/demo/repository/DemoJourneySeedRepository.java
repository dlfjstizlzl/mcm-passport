package com.mcm.passport.domain.demo.repository;

import com.mcm.passport.domain.demo.entity.DemoJourneySeed;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemoJourneySeedRepository extends JpaRepository<DemoJourneySeed, String> {
}
