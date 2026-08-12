package com.codercup.jobmatchai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(InvalidAnalysisRequestException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidAnalysisRequest(InvalidAnalysisRequestException exception) {
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(AnalysisConfigurationException.class)
	public ResponseEntity<ApiErrorResponse> handleAnalysisConfiguration(AnalysisConfigurationException exception) {
		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(AiServiceUnavailableException.class)
	public ResponseEntity<ApiErrorResponse> handleAiServiceUnavailable(AiServiceUnavailableException exception) {
		return ResponseEntity
				.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ApiErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(InvalidAiResponseException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidAiResponse(InvalidAiResponseException exception) {
		return ResponseEntity
				.status(HttpStatus.BAD_GATEWAY)
				.body(new ApiErrorResponse(exception.getMessage()));
	}

	public record ApiErrorResponse(String message) {
	}
}
