package com.mcm.passport.domain.style.controller;

import com.mcm.passport.domain.style.dto.StyleSpotSessionResponse;
import com.mcm.passport.domain.style.service.StyleSpotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/style-spot-sessions")
public class StyleSpotSessionController {

	private final StyleSpotService styleSpotService;

	public StyleSpotSessionController(StyleSpotService styleSpotService) {
		this.styleSpotService = styleSpotService;
	}

	@PostMapping("/{styleSpotSessionId}/disconnect")
	public ResponseEntity<StyleSpotSessionResponse> disconnect(@PathVariable Long styleSpotSessionId) {
		return ResponseEntity.ok(styleSpotService.disconnect(styleSpotSessionId));
	}
}
