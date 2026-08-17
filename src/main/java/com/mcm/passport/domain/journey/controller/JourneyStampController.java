package com.mcm.passport.domain.journey.controller;

import com.mcm.passport.domain.journey.dto.JourneyStampResponse;
import com.mcm.passport.domain.journey.service.JourneyStampService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/passport-sessions/{passportSessionId}/journey-spots")
public class JourneyStampController {

	private final JourneyStampService journeyStampService;

	public JourneyStampController(JourneyStampService journeyStampService) {
		this.journeyStampService = journeyStampService;
	}

	@PostMapping("/{journeySpotId}/complete")
	public ResponseEntity<JourneyStampResponse> complete(
			@PathVariable Long passportSessionId,
			@PathVariable Long journeySpotId
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(journeyStampService.complete(passportSessionId, journeySpotId));
	}
}
