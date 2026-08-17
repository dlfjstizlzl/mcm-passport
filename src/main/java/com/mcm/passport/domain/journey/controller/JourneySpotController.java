package com.mcm.passport.domain.journey.controller;

import com.mcm.passport.domain.journey.dto.JourneySpotDetailResponse;
import com.mcm.passport.domain.journey.dto.JourneySpotResponse;
import com.mcm.passport.domain.journey.service.JourneySpotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/journey-spots")
public class JourneySpotController {

	private final JourneySpotService journeySpotService;

	public JourneySpotController(JourneySpotService journeySpotService) {
		this.journeySpotService = journeySpotService;
	}

	@GetMapping
	public ResponseEntity<List<JourneySpotResponse>> getAll() {
		return ResponseEntity.ok(journeySpotService.getAll());
	}

	@GetMapping("/{journeySpotId}")
	public ResponseEntity<JourneySpotDetailResponse> get(@PathVariable Long journeySpotId) {
		return ResponseEntity.ok(journeySpotService.get(journeySpotId));
	}
}
