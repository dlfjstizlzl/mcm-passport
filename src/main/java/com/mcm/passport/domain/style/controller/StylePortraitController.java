package com.mcm.passport.domain.style.controller;

import com.mcm.passport.domain.style.dto.StylePortraitRequest;
import com.mcm.passport.domain.style.dto.StylePortraitResponse;
import com.mcm.passport.domain.style.service.StylePortraitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/passport-sessions/{passportSessionId}/portrait")
public class StylePortraitController {

	private final StylePortraitService stylePortraitService;

	public StylePortraitController(StylePortraitService stylePortraitService) {
		this.stylePortraitService = stylePortraitService;
	}

	@PostMapping
	public ResponseEntity<StylePortraitResponse> create(
			@PathVariable Long passportSessionId,
			@Valid @RequestBody StylePortraitRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(stylePortraitService.create(passportSessionId, request));
	}

	@GetMapping
	public ResponseEntity<StylePortraitResponse> get(@PathVariable Long passportSessionId) {
		return ResponseEntity.ok(stylePortraitService.get(passportSessionId));
	}

	@DeleteMapping
	public ResponseEntity<Void> delete(@PathVariable Long passportSessionId) {
		stylePortraitService.delete(passportSessionId);
		return ResponseEntity.noContent().build();
	}
}
