package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

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
}
