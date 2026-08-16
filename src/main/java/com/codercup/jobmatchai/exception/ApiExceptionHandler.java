package com.codercup.jobmatchai.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(InvalidAnalysisRequestException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidAnalysisRequest(InvalidAnalysisRequestException exception) {
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse("INVALID_REQUEST", exception.getMessage()));
	}

	@ExceptionHandler(InvalidCvContentException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidCvContent(InvalidCvContentException exception) {
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse("INVALID_CV_CONTENT", exception.getMessage()));
	}

	@ExceptionHandler(AnalysisConfigurationException.class)
	public ResponseEntity<ApiErrorResponse> handleAnalysisConfiguration(AnalysisConfigurationException exception) {
		LOGGER.error("Analysis service configuration error: {}", exception.getMessage(), exception);
		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse(
						"CONFIGURATION_ERROR",
						"El servicio de análisis no está configurado correctamente."
				));
	}

	@ExceptionHandler(AiQuotaExceededException.class)
	public ResponseEntity<ApiErrorResponse> handleAiQuotaExceeded(AiQuotaExceededException exception) {
		return ResponseEntity
				.status(HttpStatus.TOO_MANY_REQUESTS)
				.body(new ApiErrorResponse(
						"AI_QUOTA_EXCEEDED",
						"Se alcanzó el límite de uso disponible del servicio de inteligencia artificial."
				));
	}

	@ExceptionHandler(AiServiceUnavailableException.class)
	public ResponseEntity<ApiErrorResponse> handleAiServiceUnavailable(AiServiceUnavailableException exception) {
		return ResponseEntity
				.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ApiErrorResponse("AI_UNAVAILABLE", exception.getMessage()));
	}

	@ExceptionHandler(AiServiceTimeoutException.class)
	public ResponseEntity<ApiErrorResponse> handleAiServiceTimeout(AiServiceTimeoutException exception) {
		return ResponseEntity
				.status(HttpStatus.GATEWAY_TIMEOUT)
				.body(new ApiErrorResponse("AI_TIMEOUT", exception.getMessage()));
	}

	@ExceptionHandler(InvalidAiResponseException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidAiResponse(InvalidAiResponseException exception) {
		return ResponseEntity
				.status(HttpStatus.BAD_GATEWAY)
				.body(new ApiErrorResponse("AI_INVALID_RESPONSE", exception.getMessage()));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
		return ResponseEntity
				.status(HttpStatus.CONTENT_TOO_LARGE)
				.body(new ApiErrorResponse("FILE_TOO_LARGE", "El archivo enviado supera el tamaño maximo permitido."));
	}

	@ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
	public ResponseEntity<ApiErrorResponse> handleMissingRequestPart(Exception exception) {
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse("MISSING_REQUEST_DATA", "Falta información requerida para procesar la solicitud."));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
		String reason = exception.getReason() == null ? "La solicitud no pudo procesarse." : exception.getReason();
		return ResponseEntity
				.status(exception.getStatusCode())
				.body(new ApiErrorResponse("INVALID_REQUEST", reason));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
		LOGGER.error("Unexpected error while processing analysis request. Exception type: {}",
				exception.getClass().getName(), exception);
		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse("INTERNAL_ERROR", "Ocurrio un error interno al procesar la solicitud."));
	}

	public record ApiErrorResponse(String code, String message) {
	}
}
