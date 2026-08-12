package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InterruptedIOException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import com.codercup.jobmatchai.exception.AiServiceTimeoutException;
import com.codercup.jobmatchai.exception.AnalysisConfigurationException;
import com.codercup.jobmatchai.exception.AiServiceUnavailableException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Part;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class GeminiServiceTest {

	@Test
	void buildPromptHandlesLiteralPercentages() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000);
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
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000);
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
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000);
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
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000);
		HttpOptions httpOptions = buildHttpOptions(geminiService);

		assertThat(httpOptions.timeout()).contains(30000);
	}

	@Test
	void buildHttpOptionsUsesConfiguredCustomTimeout() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 5000);
		HttpOptions httpOptions = buildHttpOptions(geminiService);

		assertThat(httpOptions.timeout()).contains(5000);
	}

	@Test
	void constructorRejectsZeroTimeout() {
		assertThatThrownBy(() -> new GeminiService("test-key", "test-model", 0))
				.isInstanceOf(AnalysisConfigurationException.class)
				.hasMessage("El timeout de Gemini debe ser mayor a 0 ms.");
	}

	@Test
	void constructorRejectsNegativeTimeout() {
		assertThatThrownBy(() -> new GeminiService("test-key", "test-model", -1))
				.isInstanceOf(AnalysisConfigurationException.class)
				.hasMessage("El timeout de Gemini debe ser mayor a 0 ms.");
	}

	@Test
	void mapGeminiIOExceptionReturnsTimeoutExceptionForSocketTimeoutCause() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000);
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
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000);
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
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000);
		RuntimeException exception = mapGeminiIOException(
				geminiService,
				new GenAiIOException("Failed to execute HTTP request.", new java.io.IOException())
		);

		assertThat(exception)
				.isInstanceOf(AiServiceUnavailableException.class)
				.hasMessage("El servicio de inteligencia artificial no esta disponible temporalmente.");
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
}
