package com.mcm.passport.domain.style.controller;

import com.mcm.passport.domain.style.dto.ConnectStyleSpotRequest;
import com.mcm.passport.domain.style.dto.StyleResultResponse;
import com.mcm.passport.domain.style.dto.StyleSpotConnectResponse;
import com.mcm.passport.domain.style.dto.StyleSpotDisplayResponse;
import com.mcm.passport.domain.style.dto.StyleSpotResponse;
import com.mcm.passport.domain.style.service.StyleAnalysisService;
import com.mcm.passport.domain.style.service.StyleSpotFlowService;
import com.mcm.passport.domain.style.service.StyleSpotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/style-spots")
public class StyleSpotController {

	private final StyleSpotService styleSpotService;
	private final StyleAnalysisService styleAnalysisService;
	private final StyleSpotFlowService styleSpotFlowService;

	public StyleSpotController(
			StyleSpotService styleSpotService,
			StyleAnalysisService styleAnalysisService,
			StyleSpotFlowService styleSpotFlowService
	) {
		this.styleSpotService = styleSpotService;
		this.styleAnalysisService = styleAnalysisService;
		this.styleSpotFlowService = styleSpotFlowService;
	}

	@PostMapping("/{styleSpotId}/connect")
	public ResponseEntity<StyleSpotConnectResponse> connectAndAnalyze(
			@PathVariable String styleSpotId,
			@Valid @RequestBody ConnectStyleSpotRequest request
	) {
		return ResponseEntity.ok(styleSpotFlowService.connectAndAnalyze(
				styleSpotId,
				request.passportSessionId()
		));
	}

	@GetMapping("/{styleSpotId}/display")
	public ResponseEntity<StyleSpotDisplayResponse> display(@PathVariable String styleSpotId) {
		return ResponseEntity.ok(styleSpotFlowService.display(styleSpotId));
	}

	@PostMapping("/{spotCode}/connections")
	public ResponseEntity<StyleSpotResponse> connect(
			@PathVariable String spotCode,
			@Valid @RequestBody ConnectStyleSpotRequest request
	) {
		styleSpotService.connectSession(spotCode, request.passportSessionId());
		return ResponseEntity.ok(styleSpotService.display(spotCode));
	}

	@PostMapping("/{spotCode}/analysis")
	public ResponseEntity<StyleResultResponse> analyze(@PathVariable String spotCode) {
		return ResponseEntity.ok(styleAnalysisService.analyze(spotCode));
	}

	@GetMapping("/{spotCode}/result")
	public ResponseEntity<StyleResultResponse> getResult(@PathVariable String spotCode) {
		return ResponseEntity.ok(styleAnalysisService.getResult(spotCode));
	}

	@PostMapping("/{spotCode}/reset")
	public ResponseEntity<StyleSpotResponse> reset(@PathVariable String spotCode) {
		return ResponseEntity.ok(styleSpotService.reset(spotCode));
	}
}
