package com.mcm.passport.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(
			BusinessException exception,
			HttpServletRequest request
	) {
		ErrorCode errorCode = exception.getErrorCode();
		ErrorResponse response = ErrorResponse.of(errorCode, exception.getMessage(), request.getRequestURI());

		return ResponseEntity.status(errorCode.getStatus()).body(response);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpServletRequest request
	) {
		List<ErrorResponse.FieldError> errors = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(this::toFieldError)
				.toList();
		ErrorCode errorCode = ErrorCode.INVALID_INPUT;
		ErrorResponse response = ErrorResponse.of(
				errorCode,
				errorCode.getMessage(),
				request.getRequestURI(),
				errors
		);

		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(
			ConstraintViolationException exception,
			HttpServletRequest request
	) {
		List<ErrorResponse.FieldError> errors = exception.getConstraintViolations()
				.stream()
				.map(violation -> new ErrorResponse.FieldError(
						violation.getPropertyPath().toString(),
						violation.getMessage()
				))
				.toList();

		return invalidInput(request, errors);
	}

	@ExceptionHandler({
			MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class,
			HandlerMethodValidationException.class
	})
	public ResponseEntity<ErrorResponse> handleInvalidRequestParameter(
			Exception exception,
			HttpServletRequest request
	) {
		return invalidInput(request, List.of());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFound(
			NoResourceFoundException exception,
			HttpServletRequest request
	) {
		return fromErrorCode(ErrorCode.API_NOT_FOUND, request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotSupported(
			HttpRequestMethodNotSupportedException exception,
			HttpServletRequest request
	) {
		return fromErrorCode(ErrorCode.METHOD_NOT_ALLOWED, request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
			HttpMediaTypeNotSupportedException exception,
			HttpServletRequest request
	) {
		return fromErrorCode(ErrorCode.UNSUPPORTED_MEDIA_TYPE, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
			HttpMessageNotReadableException exception,
			HttpServletRequest request
	) {
		ErrorCode errorCode = ErrorCode.MALFORMED_JSON;
		return ResponseEntity.badRequest()
				.body(ErrorResponse.of(errorCode, errorCode.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(
			Exception exception,
			HttpServletRequest request
	) {
		log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), exception);

		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
		return ResponseEntity.status(errorCode.getStatus())
				.body(ErrorResponse.of(errorCode, errorCode.getMessage(), request.getRequestURI()));
	}

	private ResponseEntity<ErrorResponse> invalidInput(
			HttpServletRequest request,
			List<ErrorResponse.FieldError> errors
	) {
		ErrorCode errorCode = ErrorCode.INVALID_INPUT;
		ErrorResponse response = ErrorResponse.of(
				errorCode,
				errorCode.getMessage(),
				request.getRequestURI(),
				errors
		);
		return ResponseEntity.badRequest().body(response);
	}

	private ResponseEntity<ErrorResponse> fromErrorCode(
			ErrorCode errorCode,
			HttpServletRequest request
	) {
		return ResponseEntity.status(errorCode.getStatus())
				.body(ErrorResponse.of(errorCode, errorCode.getMessage(), request.getRequestURI()));
	}

	private ErrorResponse.FieldError toFieldError(FieldError fieldError) {
		return new ErrorResponse.FieldError(
				fieldError.getField(),
				fieldError.getDefaultMessage()
		);
	}
}
