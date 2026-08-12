package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.lang.reflect.Method;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class GeminiServiceTest {

	@Test
	void buildPromptHandlesLiteralPercentages() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model");
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
		GeminiService geminiService = new GeminiService("test-key", "test-model");
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
		GeminiService geminiService = new GeminiService("test-key", "test-model");
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
}
