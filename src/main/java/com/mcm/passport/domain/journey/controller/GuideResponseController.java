package com.mcm.passport.domain.journey.controller;

import com.mcm.passport.domain.journey.dto.GuideResponseRequest;
import com.mcm.passport.domain.journey.dto.GuideResponseResponse;
import com.mcm.passport.domain.journey.service.GuideResponseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/passport-sessions/{passportSessionId}/guide-responses")
public class GuideResponseController {

	private final GuideResponseService guideResponseService;

	public GuideResponseController(GuideResponseService guideResponseService) {
		this.guideResponseService = guideResponseService;
	}

	@PutMapping("/{questionId}")
	public ResponseEntity<GuideResponseResponse> saveOrUpdate(
			@PathVariable Long passportSessionId,
			@PathVariable Long questionId,
			@Valid @RequestBody GuideResponseRequest request
	) {
		return ResponseEntity.ok(guideResponseService.saveOrUpdate(passportSessionId, questionId, request));
	}
}
