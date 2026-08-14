package com.mcm.passport.global.exception;

import java.util.Objects;

public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
	}

	@Override
	public String getMessage() {
		return super.getMessage() == null ? errorCode.getMessage() : super.getMessage();
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
