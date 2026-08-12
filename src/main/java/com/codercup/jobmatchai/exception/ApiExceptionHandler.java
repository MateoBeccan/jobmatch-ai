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

	public record ApiErrorResponse(String message) {
	}
}
