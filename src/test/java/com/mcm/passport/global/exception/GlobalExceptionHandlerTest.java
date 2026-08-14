package com.mcm.passport.global.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void businessExceptionUsesDefinedStatusAndCode() {
		MockHttpServletRequest request = request("GET", "/api/passport-sessions/1");
		BusinessException exception = new BusinessException(ErrorCode.PASSPORT_SESSION_NOT_FOUND);

		ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception, request);

		assertThat(response.getStatusCode().value()).isEqualTo(404);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("PASSPORT_SESSION_NOT_FOUND");
		assertThat(response.getBody().message()).isEqualTo(ErrorCode.PASSPORT_SESSION_NOT_FOUND.getMessage());
		assertThat(response.getBody().path()).isEqualTo("/api/passport-sessions/1");
		assertThat(response.getBody().errors()).isEmpty();
	}

	@Test
	void unexpectedExceptionDoesNotExposeInternalMessage() {
		MockHttpServletRequest request = request("POST", "/api/test");

		ResponseEntity<ErrorResponse> response = handler.handleUnexpectedException(
				new IllegalStateException("database-password-leaked"),
				request
		);

		assertThat(response.getStatusCode().value()).isEqualTo(500);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("INTERNAL_SERVER_ERROR");
		assertThat(response.getBody().message()).doesNotContain("database-password-leaked");
	}

	private MockHttpServletRequest request(String method, String uri) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
		request.setRequestURI(uri);
		return request;
	}
}
