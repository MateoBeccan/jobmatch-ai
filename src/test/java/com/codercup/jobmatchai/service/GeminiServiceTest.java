package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InterruptedIOException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import com.codercup.jobmatchai.dto.AnalysisResponse;
import com.codercup.jobmatchai.exception.AiServiceTimeoutException;
import com.codercup.jobmatchai.exception.AnalysisConfigurationException;
import com.codercup.jobmatchai.exception.AiServiceUnavailableException;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.Part;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class GeminiServiceTest {

	@Test
	void buildPromptHandlesLiteralPercentages() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);
		Method buildPrompt = GeminiService.class.getDeclaredMethod("buildPrompt", String.class, String.class);
		buildPrompt.setAccessible(true);

		assertThatNoException()
				.isThrownBy(() -> buildPrompt.invoke(geminiService, "CV con Java", "Oferta con Spring Boot"));

		String prompt = (String) buildPrompt.invoke(geminiService, "CV con Java", "Oferta con Spring Boot");
		assertThat(prompt)
				.contains("60%")
				.contains("20%")
				.contains("10%")
				.contains("CV con Java")
				.contains("Oferta con Spring Boot");
	}

	@Test
	void promptsIncludeRulesForAlternativesAndSafeRecommendations() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);
		Method buildPrompt = GeminiService.class.getDeclaredMethod("buildPrompt", String.class, String.class);
		Method buildImagePrompt = GeminiService.class.getDeclaredMethod("buildImagePrompt", String.class);
		buildPrompt.setAccessible(true);
		buildImagePrompt.setAccessible(true);

		String textPrompt = (String) buildPrompt.invoke(
				geminiService,
				"CV con Java, Spring Boot, MySQL y proyecto full-stack con Vue.js",
				"Java or Kotlin. PHP or similar server-side technology."
		);
		String imagePrompt = (String) buildImagePrompt.invoke(
				geminiService,
				"CV con Java, Spring Boot, MySQL y proyecto full-stack con Vue.js"
		);

		assertPromptIncludesRulesForAlternativesAndSafeRecommendations(textPrompt);
		assertPromptIncludesRulesForAlternativesAndSafeRecommendations(imagePrompt);
	}

	@Test
	void buildImageContentUsesInlineImageAndStructuredOutput() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);
		Method buildImageContent = GeminiService.class.getDeclaredMethod(
				"buildImageContent",
				String.class,
				org.springframework.web.multipart.MultipartFile.class
		);
		Method buildConfig = GeminiService.class.getDeclaredMethod("buildConfig");
		buildImageContent.setAccessible(true);
		buildConfig.setAccessible(true);

		MockMultipartFile jobImage = new MockMultipartFile(
				"jobImage",
				"job.png",
				"image/png",
				new byte[] {1, 2, 3}
		);

		assertThatNoException()
				.isThrownBy(() -> buildImageContent.invoke(geminiService, "CV con Java", jobImage));

		Content content = (Content) buildImageContent.invoke(geminiService, "CV con Java", jobImage);
		GenerateContentConfig config = (GenerateContentConfig) buildConfig.invoke(geminiService);
		assertThat(content.parts()).isPresent();
		assertThat(content.parts().get()).hasSize(2);

		Part textPart = content.parts().get().get(0);
		Part imagePart = content.parts().get().get(1);
		assertThat(textPart.text()).isPresent();
		assertThat(textPart.text().get())
				.contains("La imagen adjunta contiene una oferta laboral")
				.contains("60%")
				.contains("20%")
				.contains("10%")
				.contains("CV con Java");
		assertThat(imagePart.inlineData()).isPresent();
		assertThat(imagePart.inlineData().get().mimeType()).contains("image/png");
		assertThat(imagePart.inlineData().get().data()).contains(new byte[] {1, 2, 3});
		assertThat(config.responseMimeType()).contains("application/json");
		assertThat(config.responseSchema()).isPresent();
	}

	@Test
	void buildHttpOptionsUsesConfiguredDefaultTimeout() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);
		HttpOptions httpOptions = buildHttpOptions(geminiService);

		assertThat(httpOptions.timeout()).contains(30000);
		assertThat(httpOptions.retryOptions()).isEmpty();
	}

	@Test
	void buildHttpOptionsUsesConfiguredCustomTimeout() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 5000, 2, 500);
		HttpOptions httpOptions = buildHttpOptions(geminiService);

		assertThat(httpOptions.timeout()).contains(5000);
	}

	@Test
	void constructorRejectsZeroTimeout() {
		assertThatThrownBy(() -> new GeminiService("test-key", "test-model", 0, 2, 500))
				.isInstanceOf(AnalysisConfigurationException.class)
				.hasMessage("El timeout de Gemini debe ser mayor a 0 ms.");
	}

	@Test
	void constructorRejectsNegativeTimeout() {
		assertThatThrownBy(() -> new GeminiService("test-key", "test-model", -1, 2, 500))
				.isInstanceOf(AnalysisConfigurationException.class)
				.hasMessage("El timeout de Gemini debe ser mayor a 0 ms.");
	}

	@Test
	void buildRetryOptionsUsesConfiguredDefaultPolicy() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);
		HttpRetryOptions retryOptions = buildRetryOptions(geminiService);

		assertThat(retryOptions.attempts()).contains(2);
		assertThat(retryOptions.httpStatusCodes()).contains(List.of(429, 502, 503));
		assertThat(retryOptions.httpStatusCodes().get()).doesNotContain(408, 504);
		assertThat(retryOptions.initialDelay()).contains(0.5);
		assertThat(retryOptions.maxDelay()).contains(0.5);
		assertThat(retryOptions.expBase()).contains(1.0);
		assertThat(retryOptions.jitter()).contains(0.0);
	}

	@Test
	void buildRetryOptionsSupportsDisablingAdditionalRetry() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 1, 500);
		HttpRetryOptions retryOptions = buildRetryOptions(geminiService);

		assertThat(retryOptions.attempts()).contains(1);
	}

	@Test
	void constructorRejectsZeroRetryAttempts() {
		assertThatThrownBy(() -> new GeminiService("test-key", "test-model", 30000, 0, 500))
				.isInstanceOf(AnalysisConfigurationException.class)
				.hasMessage("Los intentos de Gemini deben ser 1 o 2.");
	}

	@Test
	void constructorRejectsRetryAttemptsGreaterThanTwo() {
		assertThatThrownBy(() -> new GeminiService("test-key", "test-model", 30000, 3, 500))
				.isInstanceOf(AnalysisConfigurationException.class)
				.hasMessage("Los intentos de Gemini deben ser 1 o 2.");
	}

	@Test
	void constructorRejectsZeroRetryDelay() {
		assertThatThrownBy(() -> new GeminiService("test-key", "test-model", 30000, 2, 0))
				.isInstanceOf(AnalysisConfigurationException.class)
				.hasMessage("El delay de retry de Gemini debe ser mayor a 0 ms.");
	}

	@Test
	void constructorRejectsNegativeRetryDelay() {
		assertThatThrownBy(() -> new GeminiService("test-key", "test-model", 30000, 2, -1))
				.isInstanceOf(AnalysisConfigurationException.class)
				.hasMessage("El delay de retry de Gemini debe ser mayor a 0 ms.");
	}

	@Test
	void shouldRetryOnlyConfiguredTransientHttpStatusCodes() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);
		HttpRetryOptions retryOptions = buildRetryOptions(geminiService);

		assertThat(shouldRetry(geminiService, new com.google.genai.errors.ApiException(429, "RESOURCE_EXHAUSTED", ""), retryOptions))
				.isTrue();
		assertThat(shouldRetry(geminiService, new com.google.genai.errors.ApiException(502, "BAD_GATEWAY", ""), retryOptions))
				.isTrue();
		assertThat(shouldRetry(geminiService, new com.google.genai.errors.ApiException(503, "UNAVAILABLE", ""), retryOptions))
				.isTrue();
		assertThat(shouldRetry(geminiService, new com.google.genai.errors.ApiException(408, "REQUEST_TIMEOUT", ""), retryOptions))
				.isFalse();
		assertThat(shouldRetry(geminiService, new com.google.genai.errors.ApiException(504, "GATEWAY_TIMEOUT", ""), retryOptions))
				.isFalse();
	}

	@Test
	void mapGeminiIOExceptionReturnsTimeoutExceptionForSocketTimeoutCause() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);
		RuntimeException exception = mapGeminiIOException(
				geminiService,
				new GenAiIOException("Failed to execute HTTP request.", new SocketTimeoutException())
		);

		assertThat(exception)
				.isInstanceOf(AiServiceTimeoutException.class)
				.hasMessage("El servicio de inteligencia artificial tard\u00f3 demasiado en responder.");
	}

	@Test
	void mapGeminiIOExceptionReturnsTimeoutExceptionForInterruptedIoCause() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);
		RuntimeException exception = mapGeminiIOException(
				geminiService,
				new GenAiIOException("Failed to execute HTTP request.", new InterruptedIOException())
		);

		assertThat(exception)
				.isInstanceOf(AiServiceTimeoutException.class)
				.hasMessage("El servicio de inteligencia artificial tard\u00f3 demasiado en responder.");
	}

	@Test
	void mapGeminiIOExceptionReturnsUnavailableExceptionForOtherIoFailures() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);
		RuntimeException exception = mapGeminiIOException(
				geminiService,
				new GenAiIOException("Failed to execute HTTP request.", new java.io.IOException())
		);

		assertThat(exception)
				.isInstanceOf(AiServiceUnavailableException.class)
				.hasMessage("El servicio de inteligencia artificial no esta disponible temporalmente.");
	}

	@Test
	void analyzeRetriesOnceWhenHttp503IsFollowedBySuccess() {
		RetryBehaviorGeminiService geminiService = new RetryBehaviorGeminiService(
				2,
				apiException(503),
				validResponseJson()
		);

		AnalysisResponse response = geminiService.analyze("CV con Java", "Oferta con Java");

		assertThat(geminiService.attempts()).isEqualTo(2);
		assertThat(response.matchPercentage()).isEqualTo(72);
		assertThat(response.matchingSkills()).containsExactly("Java");
	}

	@Test
	void analyzeStopsAfterTwoHttp503Errors() {
		RetryBehaviorGeminiService geminiService = new RetryBehaviorGeminiService(
				2,
				apiException(503),
				apiException(503),
				new AssertionError("No debe existir un tercer intento")
		);

		assertThatThrownBy(() -> geminiService.analyze("CV con Java", "Oferta con Java"))
				.isInstanceOf(AiServiceUnavailableException.class)
				.hasMessage("El servicio de inteligencia artificial no esta disponible temporalmente.");
		assertThat(geminiService.attempts()).isEqualTo(2);
	}

	@Test
	void analyzeDoesNotRetryNonRetryableHttp400() {
		RetryBehaviorGeminiService geminiService = new RetryBehaviorGeminiService(
				2,
				apiException(400),
				new AssertionError("No debe existir un segundo intento")
		);

		assertThatThrownBy(() -> geminiService.analyze("CV con Java", "Oferta con Java"))
				.isInstanceOf(AiServiceUnavailableException.class)
				.hasMessage("El servicio de inteligencia artificial no esta disponible temporalmente.");
		assertThat(geminiService.attempts()).isEqualTo(1);
	}

	@Test
	void analyzeDoesNotRetryTimeout() {
		RetryBehaviorGeminiService geminiService = new RetryBehaviorGeminiService(
				2,
				new GenAiIOException("Failed to execute HTTP request.", new SocketTimeoutException()),
				new AssertionError("No debe existir un segundo intento")
		);

		assertThatThrownBy(() -> geminiService.analyze("CV con Java", "Oferta con Java"))
				.isInstanceOf(AiServiceTimeoutException.class)
				.hasMessage("El servicio de inteligencia artificial tard\u00f3 demasiado en responder.");
		assertThat(geminiService.attempts()).isEqualTo(1);
	}

	@Test
	void analyzeDoesNotRetryHttp503WhenRetryAttemptsIsOne() {
		RetryBehaviorGeminiService geminiService = new RetryBehaviorGeminiService(
				1,
				apiException(503),
				new AssertionError("No debe existir un segundo intento")
		);

		assertThatThrownBy(() -> geminiService.analyze("CV con Java", "Oferta con Java"))
				.isInstanceOf(AiServiceUnavailableException.class)
				.hasMessage("El servicio de inteligencia artificial no esta disponible temporalmente.");
		assertThat(geminiService.attempts()).isEqualTo(1);
	}

	private void assertPromptIncludesRulesForAlternativesAndSafeRecommendations(String prompt) {
		String normalizedPrompt = prompt.replaceAll("\\s+", " ");

		assertThat(normalizedPrompt)
				.contains("Java or Kotlin")
				.contains("no agregues Kotlin a missingSkills")
				.contains("PHP or similar server-side technology")
				.contains("alternativa server-side razonablemente equivalente")
				.contains("Nunca recomiendes agregar al CV una habilidad")
				.contains("que no este demostrada")
				.contains("proyecto academico o personal")
				.contains("no debe convertirse")
				.contains("anos de experiencia profesional");
	}

	private HttpOptions buildHttpOptions(GeminiService geminiService) throws Exception {
		Method buildHttpOptions = GeminiService.class.getDeclaredMethod("buildHttpOptions");
		buildHttpOptions.setAccessible(true);
		return (HttpOptions) buildHttpOptions.invoke(geminiService);
	}

	private HttpRetryOptions buildRetryOptions(GeminiService geminiService) throws Exception {
		Method buildRetryOptions = GeminiService.class.getDeclaredMethod("buildRetryOptions");
		buildRetryOptions.setAccessible(true);
		return (HttpRetryOptions) buildRetryOptions.invoke(geminiService);
	}

	private boolean shouldRetry(
			GeminiService geminiService,
			com.google.genai.errors.ApiException exception,
			HttpRetryOptions retryOptions
	) throws Exception {
		Method shouldRetry = GeminiService.class.getDeclaredMethod(
				"shouldRetry",
				com.google.genai.errors.ApiException.class,
				HttpRetryOptions.class
		);
		shouldRetry.setAccessible(true);
		return (boolean) shouldRetry.invoke(geminiService, exception, retryOptions);
	}

	private RuntimeException mapGeminiIOException(
			GeminiService geminiService,
			GenAiIOException exception
	) throws Exception {
		Method mapGeminiIOException = GeminiService.class.getDeclaredMethod(
				"mapGeminiIOException",
				GenAiIOException.class
		);
		mapGeminiIOException.setAccessible(true);
		return (RuntimeException) mapGeminiIOException.invoke(geminiService, exception);
	}

	private ApiException apiException(int statusCode) {
		return new ApiException(statusCode, "ERROR", "");
	}

	private String validResponseJson() {
		return """
				{
				  "matchPercentage": 72,
				  "matchingSkills": ["Java"],
				  "missingSkills": ["Docker"],
				  "recommendations": ["Practicar Docker", "Destacar experiencia con Java"],
				  "interviewQuestions": ["Pregunta 1", "Pregunta 2", "Pregunta 3"]
				}
				""";
	}

	private static class RetryBehaviorGeminiService extends GeminiService {

		private final Queue<Object> outcomes;
		private int attempts;

		RetryBehaviorGeminiService(int retryAttempts, Object... outcomes) {
			super("test-key", "test-model", 30000, retryAttempts, 500);
			this.outcomes = new ArrayDeque<>(List.of(outcomes));
		}

		int attempts() {
			return attempts;
		}

		@Override
		String generateContentOnce(GeminiContentCall contentCall) {
			attempts++;
			Object outcome = outcomes.remove();
			if (outcome instanceof RuntimeException exception) {
				throw exception;
			}
			if (outcome instanceof Error error) {
				throw error;
			}
			return (String) outcome;
		}

		@Override
		void pauseBeforeRetry(HttpRetryOptions retryOptions) {
		}
	}
}
