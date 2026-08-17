package com.mcm.passport.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	MALFORMED_JSON(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다."),
	API_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 API를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),

	PASSPORT_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "Passport Card를 찾을 수 없습니다."),
	PASSPORT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Passport Session을 찾을 수 없습니다."),
	JOURNEY_SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "Journey Spot을 찾을 수 없습니다."),
	GUIDE_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Guide Question을 찾을 수 없습니다."),
	GUIDE_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Guide Option을 찾을 수 없습니다."),
	PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product를 찾을 수 없습니다."),
	BOARDING_PASS_NOT_FOUND(HttpStatus.NOT_FOUND, "Boarding Pass를 찾을 수 없습니다."),
	STYLE_SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "Style Spot을 찾을 수 없습니다."),
	STYLE_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "Style Result를 찾을 수 없습니다."),
	STYLE_PORTRAIT_NOT_FOUND(HttpStatus.NOT_FOUND, "Style Portrait을 찾을 수 없습니다."),
	JOURNEY_SOUVENIR_NOT_FOUND(HttpStatus.NOT_FOUND, "Journey Souvenir를 찾을 수 없습니다."),

	INVALID_GUIDE_OPTION(HttpStatus.BAD_REQUEST, "선택한 Option이 해당 Question에 속하지 않습니다."),

	JOURNEY_NOT_COMPLETED(HttpStatus.CONFLICT, "필수 Journey 조건이 완료되지 않았습니다."),
	PASSPORT_CARD_INACTIVE(HttpStatus.CONFLICT, "비활성화된 Passport Card입니다."),
	ACTIVE_PASSPORT_SESSION_EXISTS(HttpStatus.CONFLICT, "이미 진행 중인 Passport Session이 존재합니다."),
	INVALID_SESSION_STATUS(HttpStatus.CONFLICT, "현재 Passport Session 상태에서는 요청을 수행할 수 없습니다."),
	JOURNEY_STAMP_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 Journey Spot의 Stamp가 이미 발급되었습니다."),
	PRODUCT_ALREADY_TAGGED(HttpStatus.CONFLICT, "해당 Product가 이미 태그되었습니다."),
	STYLE_SPOT_IN_USE(HttpStatus.CONFLICT, "Style Spot이 이미 사용 중입니다."),
	DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 생성된 리소스입니다."),

	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}
}
