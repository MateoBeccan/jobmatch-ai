package com.codercup.jobmatchai.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
		assertThat(response.getBody().message()).isEqualTo("El archivo enviado supera el tamaño maximo permitido.");
	}

	@Test
	void handleUnexpectedExceptionReturnsGenericMessage() {
		ResponseEntity<ApiExceptionHandler.ApiErrorResponse> response = handler.handleUnexpectedException(
				new IllegalStateException("sensitive internal detail")
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().message()).isEqualTo("Ocurrio un error interno al procesar la solicitud.");
		assertThat(response.getBody().message()).doesNotContain("sensitive internal detail");
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
		assertThat(response.getBody().message())
				.isEqualTo("El servicio de inteligencia artificial tard\u00f3 demasiado en responder.");
	}
}
