package com.mcm.passport.domain.journey.controller;

import com.mcm.passport.domain.journey.dto.JourneyProgressResponse;
import com.mcm.passport.domain.journey.service.JourneyProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/passport-sessions/{passportSessionId}/journey")
public class JourneyProgressController {
	private final JourneyProgressService journeyProgressService;
	public JourneyProgressController(JourneyProgressService journeyProgressService) {
		this.journeyProgressService = journeyProgressService;
	}

	@GetMapping
	public ResponseEntity<JourneyProgressResponse> get(@PathVariable Long passportSessionId) {
		return ResponseEntity.ok(journeyProgressService.get(passportSessionId));
	}
}
