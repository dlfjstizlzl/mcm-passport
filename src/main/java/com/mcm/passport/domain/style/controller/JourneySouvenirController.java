package com.mcm.passport.domain.style.controller;

import com.mcm.passport.domain.style.dto.JourneySouvenirResponse;
import com.mcm.passport.domain.style.service.JourneySouvenirCreation;
import com.mcm.passport.domain.style.service.JourneySouvenirService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/passport-sessions/{passportSessionId}/souvenir")
public class JourneySouvenirController {

	private final JourneySouvenirService journeySouvenirService;

	public JourneySouvenirController(JourneySouvenirService journeySouvenirService) {
		this.journeySouvenirService = journeySouvenirService;
	}

	@PostMapping
	public ResponseEntity<JourneySouvenirResponse> create(@PathVariable Long passportSessionId) {
		JourneySouvenirCreation creation = journeySouvenirService.create(passportSessionId);
		HttpStatus status = creation.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(creation.souvenir());
	}

	@GetMapping
	public ResponseEntity<JourneySouvenirResponse> get(@PathVariable Long passportSessionId) {
		return ResponseEntity.ok(journeySouvenirService.get(passportSessionId));
	}
}
