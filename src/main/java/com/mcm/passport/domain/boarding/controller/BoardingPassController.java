package com.mcm.passport.domain.boarding.controller;

import com.mcm.passport.domain.boarding.dto.BoardingPassResponse;
import com.mcm.passport.domain.boarding.service.BoardingPassService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/passport-sessions/{passportSessionId}/boarding-pass")
public class BoardingPassController {
	private final BoardingPassService boardingPassService;
	public BoardingPassController(BoardingPassService boardingPassService) { this.boardingPassService = boardingPassService; }

	@PostMapping
	public ResponseEntity<BoardingPassResponse> issue(@PathVariable Long passportSessionId) {
		return ResponseEntity.status(HttpStatus.CREATED).body(boardingPassService.issue(passportSessionId));
	}

	@GetMapping
	public ResponseEntity<BoardingPassResponse> get(@PathVariable Long passportSessionId) {
		return ResponseEntity.ok(boardingPassService.get(passportSessionId));
	}
}
