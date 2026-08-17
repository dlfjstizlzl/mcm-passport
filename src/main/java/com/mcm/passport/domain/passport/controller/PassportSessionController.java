package com.mcm.passport.domain.passport.controller;

import com.mcm.passport.domain.passport.dto.PassportSessionCreateRequest;
import com.mcm.passport.domain.passport.dto.PassportSessionCreateResponse;
import com.mcm.passport.domain.passport.dto.PassportSessionResponse;
import com.mcm.passport.domain.passport.service.PassportSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/passport-sessions")
public class PassportSessionController {

	private final PassportSessionService passportSessionService;

	public PassportSessionController(PassportSessionService passportSessionService) {
		this.passportSessionService = passportSessionService;
	}

	@PostMapping
	public ResponseEntity<PassportSessionCreateResponse> create(
			@Valid @RequestBody PassportSessionCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(passportSessionService.create(request));
	}

	@GetMapping("/{passportSessionId}")
	public ResponseEntity<PassportSessionResponse> get(
			@PathVariable Long passportSessionId
	) {
		return ResponseEntity.ok(passportSessionService.get(passportSessionId));
	}
}
