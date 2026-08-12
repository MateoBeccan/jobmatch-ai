package com.codercup.jobmatchai.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

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

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
		return ResponseEntity
				.status(HttpStatus.CONTENT_TOO_LARGE)
				.body(new ApiErrorResponse("El archivo enviado supera el tamaño maximo permitido."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
		LOGGER.error("Unexpected error while processing analysis request. Exception type: {}",
				exception.getClass().getName());
		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse("Ocurrio un error interno al procesar la solicitud."));
	}

	public record ApiErrorResponse(String message) {
	}
}
