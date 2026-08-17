package com.mcm.passport.domain.style.controller;

import com.mcm.passport.domain.style.dto.StyleResultResponse;
import com.mcm.passport.domain.style.service.PassportStyleResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/passport-sessions/{passportSessionId}/style-result")
public class PassportStyleResultController {

	private final PassportStyleResultService passportStyleResultService;

	public PassportStyleResultController(PassportStyleResultService passportStyleResultService) {
		this.passportStyleResultService = passportStyleResultService;
	}

	@GetMapping
	public ResponseEntity<StyleResultResponse> get(@PathVariable Long passportSessionId) {
		return ResponseEntity.ok(passportStyleResultService.get(passportSessionId));
	}
}
