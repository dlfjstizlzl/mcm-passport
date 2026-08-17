package com.mcm.passport.domain.style.controller;

import com.mcm.passport.domain.style.dto.MyPassportResponse;
import com.mcm.passport.domain.style.service.MyPassportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/passport-sessions/{passportSessionId}/my-passport")
public class MyPassportController {

	private final MyPassportService myPassportService;

	public MyPassportController(MyPassportService myPassportService) {
		this.myPassportService = myPassportService;
	}

	@GetMapping
	public ResponseEntity<MyPassportResponse> get(@PathVariable Long passportSessionId) {
		return ResponseEntity.ok(myPassportService.get(passportSessionId));
	}
}
