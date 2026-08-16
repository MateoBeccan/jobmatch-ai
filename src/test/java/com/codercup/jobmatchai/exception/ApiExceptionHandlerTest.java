package com.codercup.jobmatchai.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class ApiExceptionHandlerTest {

	private final ApiExceptionHandler handler = new ApiExceptionHandler();

	@Test
	void handleMaxUploadSizeExceededReturnsPayloadTooLargeWithoutInternalDetails() {
		ResponseEntity<ApiExceptionHandler.ApiErrorResponse> response = handler.handleMaxUploadSizeExceeded(
				new MaxUploadSizeExceededException(5L * 1024L * 1024L)
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("FILE_TOO_LARGE");
		assertThat(response.getBody().message()).isEqualTo("El archivo enviado supera el tamaño maximo permitido.");
	}

	@Test
	void handleInvalidAnalysisRequestReturnsInvalidRequestCode() {
		ResponseEntity<ApiExceptionHandler.ApiErrorResponse> response = handler.handleInvalidAnalysisRequest(
				new InvalidAnalysisRequestException("El archivo del CV no puede estar vacio.")
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
		assertThat(response.getBody().message()).isEqualTo("El archivo del CV no puede estar vacio.");
	}

	@Test
	void handleMissingRequestPartReturnsMissingRequestDataCode() throws Exception {
		ResponseEntity<ApiExceptionHandler.ApiErrorResponse> response = handler.handleMissingRequestPart(
				new MissingServletRequestParameterException("cvFile", "MultipartFile")
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("MISSING_REQUEST_DATA");
		assertThat(response.getBody().message()).isEqualTo("Falta información requerida para procesar la solicitud.");
	}

	@Test
	void handleAiServiceUnavailableReturnsServiceUnavailableCode() {
		ResponseEntity<ApiExceptionHandler.ApiErrorResponse> response = handler.handleAiServiceUnavailable(
				new AiServiceUnavailableException("El servicio de inteligencia artificial no esta disponible temporalmente.")
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("AI_UNAVAILABLE");
		assertThat(response.getBody().message())
				.isEqualTo("El servicio de inteligencia artificial no esta disponible temporalmente.");
	}

	@Test
	void handleUnexpectedExceptionReturnsGenericMessage() {
		ResponseEntity<ApiExceptionHandler.ApiErrorResponse> response = handler.handleUnexpectedException(
				new IllegalStateException("sensitive internal detail")
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
		assertThat(response.getBody().message()).isEqualTo("Ocurrio un error interno al procesar la solicitud.");
		assertThat(response.getBody().message()).doesNotContain("sensitive internal detail");
	}

	@Test
	void handleAnalysisConfigurationReturnsGenericMessageWithoutInternalDetails() {
		ResponseEntity<ApiExceptionHandler.ApiErrorResponse> response = handler.handleAnalysisConfiguration(
				new AnalysisConfigurationException("Falta GEMINI_API_KEY. Agrega tu clave.")
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("CONFIGURATION_ERROR");
		assertThat(response.getBody().message()).isEqualTo("El servicio de análisis no está configurado correctamente.");
		assertThat(response.getBody().message()).doesNotContain("GEMINI_API_KEY");
	}

	@Test
	void handleAiServiceTimeoutReturnsGatewayTimeout() {
		ResponseEntity<ApiExceptionHandler.ApiErrorResponse> response = handler.handleAiServiceTimeout(
				new AiServiceTimeoutException(
						"El servicio de inteligencia artificial tard\u00f3 demasiado en responder.",
						new SocketTimeoutException()
				)
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("AI_TIMEOUT");
		assertThat(response.getBody().message())
				.isEqualTo("El servicio de inteligencia artificial tard\u00f3 demasiado en responder.");
	}

	@Test
	void handleInvalidAiResponseReturnsBadGatewayCode() {
		ResponseEntity<ApiExceptionHandler.ApiErrorResponse> response = handler.handleInvalidAiResponse(
				new InvalidAiResponseException("No se pudo interpretar la respuesta del servicio de analisis.")
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("AI_INVALID_RESPONSE");
		assertThat(response.getBody().message())
				.isEqualTo("No se pudo interpretar la respuesta del servicio de analisis.");
	}

	@Test
	void handleAiQuotaExceededReturnsTooManyRequestsCode() {
		ResponseEntity<ApiExceptionHandler.ApiErrorResponse> response = handler.handleAiQuotaExceeded(
				new AiQuotaExceededException(
						"raw provider message",
						new RuntimeException("quota id: secret")
				)
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("AI_QUOTA_EXCEEDED");
		assertThat(response.getBody().message())
				.isEqualTo("Se alcanzó el límite de uso disponible del servicio de inteligencia artificial.");
		assertThat(response.getBody().message()).doesNotContain("quota id");
	}
}
