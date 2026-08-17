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
import com.codercup.jobmatchai.dto.internal.GeminiAnalysisResult;
import com.codercup.jobmatchai.dto.JobSeniority;
import com.codercup.jobmatchai.exception.AiQuotaExceededException;
import com.codercup.jobmatchai.exception.AiServiceTimeoutException;
import com.codercup.jobmatchai.exception.AnalysisConfigurationException;
import com.codercup.jobmatchai.exception.AiServiceUnavailableException;
import com.codercup.jobmatchai.exception.InvalidAiResponseException;
import com.codercup.jobmatchai.scoring.RequirementCategory;
import com.codercup.jobmatchai.scoring.RequirementStatus;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class GeminiServiceTest {

	@Test
	void buildPromptDelegatesPercentageCalculationToJava() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);
		Method buildPrompt = GeminiService.class.getDeclaredMethod("buildPrompt", String.class, String.class);
		buildPrompt.setAccessible(true);

		assertThatNoException()
				.isThrownBy(() -> buildPrompt.invoke(geminiService, "CV con Java", "Oferta con Spring Boot"));

		String prompt = (String) buildPrompt.invoke(geminiService, "CV con Java", "Oferta con Spring Boot");
		assertThat(prompt)
				.contains("Java calculara el porcentaje final")
				.contains("MANDATORY_TECHNICAL")
				.contains("EXPERIENCE_SENIORITY")
				.contains("DESIRABLE")
				.contains("COMPLEMENTARY")
				.contains("MATCH")
				.contains("PARTIAL")
				.contains("MISSING")
				.contains("CV con Java")
				.contains("Oferta con Spring Boot")
				.contains("jobSearchProfile")
				.contains("seniority exclusivamente")
				.contains("no debe modificar el seniority")
				.contains("Deben estar demostrados por el CV")
				.doesNotContain("matchPercentage")
				.doesNotContain("60%")
				.doesNotContain("20%")
				.doesNotContain("10%");
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
				.contains("requirements")
				.contains("jobSearchProfile")
				.contains("seniority exclusivamente")
				.contains("no debe modificar el seniority")
				.contains("No incluyas tecnologias que solo aparecen en la oferta")
				.contains("MANDATORY_TECHNICAL")
				.contains("CV con Java")
				.doesNotContain("matchPercentage")
				.doesNotContain("60%")
				.doesNotContain("20%")
				.doesNotContain("10%");
		assertThat(imagePart.inlineData()).isPresent();
		assertThat(imagePart.inlineData().get().mimeType()).contains("image/png");
		assertThat(imagePart.inlineData().get().data()).contains(new byte[] {1, 2, 3});
		assertThat(config.responseMimeType()).contains("application/json");
		assertThat(config.responseSchema()).isPresent();
		assertThat(config.seed()).contains(42);
		assertThat(config.temperature()).isEmpty();
		assertThat(config.topP()).isEmpty();
		assertThat(config.topK()).isEmpty();
	}

	@Test
	void buildConfigUsesFixedSeedWithoutSamplingParameters() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);
		GenerateContentConfig config = buildConfig(geminiService);

		assertThat(config.responseMimeType()).contains("application/json");
		assertThat(config.responseSchema()).isPresent();
		assertThat(config.seed()).contains(42);
		assertThat(config.temperature()).isEmpty();
		assertThat(config.topP()).isEmpty();
		assertThat(config.topK()).isEmpty();
	}

	@Test
	void promptsIncludeStableRequirementExtractionRules() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);
		Method buildPrompt = GeminiService.class.getDeclaredMethod("buildPrompt", String.class, String.class);
		Method buildImagePrompt = GeminiService.class.getDeclaredMethod("buildImagePrompt", String.class);
		buildPrompt.setAccessible(true);
		buildImagePrompt.setAccessible(true);

		String textPrompt = (String) buildPrompt.invoke(geminiService, "CV con Java", "Java y Spring Boot");
		String imagePrompt = (String) buildImagePrompt.invoke(geminiService, "CV con Java");

		assertPromptIncludesStableRequirementExtractionRules(textPrompt);
		assertPromptIncludesStableRequirementExtractionRules(imagePrompt);
	}

	@Test
	void responseSchemaUsesRequirementsAndDoesNotExposeMatchPercentage() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);
		Schema schema = buildResponseSchema(geminiService);

		assertThat(schema.properties()).isPresent();
		assertThat(schema.properties().get()).containsKeys(
				"requirements",
				"matchingSkills",
				"missingSkills",
				"recommendations",
				"interviewQuestions",
				"jobSearchProfile"
		);
		assertThat(schema.properties().get()).doesNotContainKey("matchPercentage");
		assertThat(schema.required()).contains(List.of(
				"requirements",
				"matchingSkills",
				"missingSkills",
				"recommendations",
				"interviewQuestions",
				"jobSearchProfile"
		));

		Schema requirementSchema = schema.properties().get().get("requirements").items().get();
		assertThat(requirementSchema.required()).contains(List.of("name", "category", "status", "evidence"));
		assertThat(requirementSchema.properties().get().get("category").enum_()).contains(List.of(
				"MANDATORY_TECHNICAL",
				"EXPERIENCE_SENIORITY",
				"DESIRABLE",
				"COMPLEMENTARY"
		));
		assertThat(requirementSchema.properties().get().get("status").enum_()).contains(List.of(
				"MATCH",
				"PARTIAL",
				"MISSING"
		));

		Schema jobSearchProfileSchema = schema.properties().get().get("jobSearchProfile");
		assertThat(jobSearchProfileSchema.required()).contains(List.of("role", "seniority", "keywords"));
		assertThat(jobSearchProfileSchema.properties().get().get("seniority").enum_()).contains(List.of(
				"TRAINEE",
				"JUNIOR",
				"MID",
				"SENIOR",
				"UNSPECIFIED"
		));
		assertThat(jobSearchProfileSchema.properties().get().get("keywords").minItems()).contains(3L);
		assertThat(jobSearchProfileSchema.properties().get().get("keywords").maxItems()).contains(6L);
	}

	@Test
	void parseResponseAcceptsValidRequirementsAndNormalizesNullableLists() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		GeminiAnalysisResult result = parseResponse(geminiService, """
				{
				  "requirements": [
				    {
				      "name": "Java",
				      "category": "MANDATORY_TECHNICAL",
				      "status": "MATCH",
				      "evidence": "Java aparece en skills y proyectos."
				    }
				  ],
				  "matchingSkills": ["Java"],
				  "missingSkills": null,
				  "recommendations": null,
				  "interviewQuestions": null,
				  "jobSearchProfile": {
				    "role": " Java Backend Developer ",
				    "seniority": "JUNIOR",
				    "keywords": ["Java", "Spring Boot", "SQL", "REST API"]
				  }
				}
				""");

		assertThat(result.requirements()).hasSize(1);
		assertThat(result.requirements().get(0).name()).isEqualTo("Java");
		assertThat(result.requirements().get(0).category()).isEqualTo(RequirementCategory.MANDATORY_TECHNICAL);
		assertThat(result.requirements().get(0).status()).isEqualTo(RequirementStatus.MATCH);
		assertThat(result.matchingSkills()).containsExactly("Java");
		assertThat(result.missingSkills()).isEmpty();
		assertThat(result.recommendations()).isEmpty();
		assertThat(result.interviewQuestions()).isEmpty();
		assertThat(result.jobSearchProfile().role()).isEqualTo("Java Backend Developer");
		assertThat(result.jobSearchProfile().seniority()).isEqualTo(JobSeniority.JUNIOR);
		assertThat(result.jobSearchProfile().keywords()).containsExactly("Java", "Spring Boot", "SQL", "REST API");
	}

	@Test
	void parseResponseRejectsNullJobSearchProfile() {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		assertThatThrownBy(() -> parseResponse(geminiService, responseWithProfile("null")))
				.isInstanceOf(InvalidAiResponseException.class)
				.hasMessage("No se pudo interpretar la respuesta del servicio de analisis.");
	}

	@Test
	void parseResponseRejectsBlankProfileRole() {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		assertThatThrownBy(() -> parseResponse(geminiService, responseWithProfile("""
				{
				  "role": "   ",
				  "seniority": "JUNIOR",
				  "keywords": ["Java", "Spring Boot", "SQL"]
				}
				""")))
				.isInstanceOf(InvalidAiResponseException.class)
				.hasMessage("No se pudo interpretar la respuesta del servicio de analisis.");
	}

	@Test
	void parseResponseRejectsUnknownProfileSeniority() {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		assertThatThrownBy(() -> parseResponse(geminiService, responseWithProfile("""
				{
				  "role": "Java Backend Developer",
				  "seniority": "LEAD",
				  "keywords": ["Java", "Spring Boot", "SQL"]
				}
				""")))
				.isInstanceOf(InvalidAiResponseException.class)
				.hasMessage("No se pudo interpretar la respuesta del servicio de analisis.");
	}

	@Test
	void parseResponseRejectsNullProfileKeywords() {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		assertThatThrownBy(() -> parseResponse(geminiService, responseWithProfile("""
				{
				  "role": "Java Backend Developer",
				  "seniority": "JUNIOR",
				  "keywords": null
				}
				""")))
				.isInstanceOf(InvalidAiResponseException.class)
				.hasMessage("No se pudo interpretar la respuesta del servicio de analisis.");
	}

	@Test
	void parseResponseRejectsProfileWithFewerThanThreeKeywords() {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		assertThatThrownBy(() -> parseResponse(geminiService, responseWithProfile("""
				{
				  "role": "Java Backend Developer",
				  "seniority": "JUNIOR",
				  "keywords": ["Java", "SQL"]
				}
				""")))
				.isInstanceOf(InvalidAiResponseException.class)
				.hasMessage("No se pudo interpretar la respuesta del servicio de analisis.");
	}

	@Test
	void parseResponseTrimsDeduplicatesAndLimitsProfileKeywords() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		GeminiAnalysisResult result = parseResponse(geminiService, responseWithProfile("""
				{
				  "role": "Java Backend Developer",
				  "seniority": "JUNIOR",
				  "keywords": [" Java ", "java", "Spring Boot", " SQL ", "", "REST API", "Git", "MySQL", "JUnit"]
				}
				"""));

		assertThat(result.jobSearchProfile().keywords())
				.containsExactly("Java", "Spring Boot", "SQL", "REST API", "Git", "MySQL");
	}

	@Test
	void parseResponseRejectsProfileWhenDeduplicationLeavesFewerThanThreeKeywords() {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		assertThatThrownBy(() -> parseResponse(geminiService, responseWithProfile("""
				{
				  "role": "Java Backend Developer",
				  "seniority": "JUNIOR",
				  "keywords": ["Java", " java ", "SQL"]
				}
				""")))
				.isInstanceOf(InvalidAiResponseException.class)
				.hasMessage("No se pudo interpretar la respuesta del servicio de analisis.");
	}

	@Test
	void parseResponseRejectsProfileRoleLongerThanDefensiveLimit() {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		assertThatThrownBy(() -> parseResponse(geminiService, responseWithProfile("""
				{
				  "role": "%s",
				  "seniority": "JUNIOR",
				  "keywords": ["Java", "Spring Boot", "SQL"]
				}
				""".formatted("A".repeat(81)))))
				.isInstanceOf(InvalidAiResponseException.class)
				.hasMessage("No se pudo interpretar la respuesta del servicio de analisis.");
	}

	@Test
	void parseResponseRejectsProfileKeywordLongerThanDefensiveLimit() {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		assertThatThrownBy(() -> parseResponse(geminiService, responseWithProfile("""
				{
				  "role": "Java Backend Developer",
				  "seniority": "JUNIOR",
				  "keywords": ["Java", "Spring Boot", "%s"]
				}
				""".formatted("A".repeat(51)))))
				.isInstanceOf(InvalidAiResponseException.class)
				.hasMessage("No se pudo interpretar la respuesta del servicio de analisis.");
	}

	@Test
	void parseResponseRejectsInvalidJson() {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		assertThatThrownBy(() -> parseResponse(geminiService, "{not json"))
				.isInstanceOf(InvalidAiResponseException.class)
				.hasMessage("No se pudo interpretar la respuesta del servicio de analisis.");
	}

	@Test
	void parseResponseRejectsUnknownCategory() {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		assertThatThrownBy(() -> parseResponse(geminiService, responseWithRequirement(
				"Java",
				"TECH",
				"MATCH",
				"Java aparece en el CV."
		)))
				.isInstanceOf(InvalidAiResponseException.class)
				.hasMessage("No se pudo interpretar la respuesta del servicio de analisis.");
	}

	@Test
	void parseResponseRejectsUnknownStatus() {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		assertThatThrownBy(() -> parseResponse(geminiService, responseWithRequirement(
				"Java",
				"MANDATORY_TECHNICAL",
				"FULL_MATCH",
				"Java aparece en el CV."
		)))
				.isInstanceOf(InvalidAiResponseException.class)
				.hasMessage("No se pudo interpretar la respuesta del servicio de analisis.");
	}

	@Test
	void parseResponseRejectsRequirementWithoutName() {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		assertThatThrownBy(() -> parseResponse(geminiService, """
				{
				  "requirements": [
				    {
				      "category": "MANDATORY_TECHNICAL",
				      "status": "MATCH",
				      "evidence": "Java aparece en el CV."
				    }
				  ],
				  "matchingSkills": [],
				  "missingSkills": [],
				  "recommendations": [],
				  "interviewQuestions": [],
				  "jobSearchProfile": {
				    "role": "Java Backend Developer",
				    "seniority": "JUNIOR",
				    "keywords": ["Java", "Spring Boot", "SQL"]
				  }
				}
				"""))
				.isInstanceOf(InvalidAiResponseException.class)
				.hasMessage("No se pudo interpretar la respuesta del servicio de analisis.");
	}

	@Test
	void parseResponseRejectsRequirementWithoutCategory() {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		assertThatThrownBy(() -> parseResponse(geminiService, """
				{
				  "requirements": [
				    {
				      "name": "Java",
				      "status": "MATCH",
				      "evidence": "Java aparece en el CV."
				    }
				  ],
				  "matchingSkills": [],
				  "missingSkills": [],
				  "recommendations": [],
				  "interviewQuestions": [],
				  "jobSearchProfile": {
				    "role": "Java Backend Developer",
				    "seniority": "JUNIOR",
				    "keywords": ["Java", "Spring Boot", "SQL"]
				  }
				}
				"""))
				.isInstanceOf(InvalidAiResponseException.class)
				.hasMessage("No se pudo interpretar la respuesta del servicio de analisis.");
	}

	@Test
	void parseResponseRejectsRequirementWithoutStatus() {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		assertThatThrownBy(() -> parseResponse(geminiService, """
				{
				  "requirements": [
				    {
				      "name": "Java",
				      "category": "MANDATORY_TECHNICAL",
				      "evidence": "Java aparece en el CV."
				    }
				  ],
				  "matchingSkills": [],
				  "missingSkills": [],
				  "recommendations": [],
				  "interviewQuestions": [],
				  "jobSearchProfile": {
				    "role": "Java Backend Developer",
				    "seniority": "JUNIOR",
				    "keywords": ["Java", "Spring Boot", "SQL"]
				  }
				}
				"""))
				.isInstanceOf(InvalidAiResponseException.class)
				.hasMessage("No se pudo interpretar la respuesta del servicio de analisis.");
	}

	@Test
	void parseResponseAllowsBlankEvidence() throws Exception {
		GeminiService geminiService = new GeminiService("test-key", "test-model", 30000, 2, 500);

		GeminiAnalysisResult result = parseResponse(geminiService, responseWithRequirement(
				"Java",
				"MANDATORY_TECHNICAL",
				"MATCH",
				""
		));

		assertThat(result.requirements()).hasSize(1);
		assertThat(result.requirements().get(0).evidence()).isEmpty();
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

		GeminiAnalysisResult response = geminiService.analyze("CV con Java", "Oferta con Java");

		assertThat(geminiService.attempts()).isEqualTo(2);
		assertThat(response.requirements()).hasSize(1);
		assertThat(response.requirements().get(0).category()).isEqualTo(RequirementCategory.MANDATORY_TECHNICAL);
		assertThat(response.requirements().get(0).status()).isEqualTo(RequirementStatus.MATCH);
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
	void analyzeMapsGeminiHttp429AfterRetriesToQuotaExceeded() {
		RetryBehaviorGeminiService geminiService = new RetryBehaviorGeminiService(
				2,
				apiException(429),
				apiException(429),
				new AssertionError("No debe existir un tercer intento")
		);

		assertThatThrownBy(() -> geminiService.analyze("CV con Java", "Oferta con Java"))
				.isInstanceOf(AiQuotaExceededException.class)
				.hasMessage("Se alcanzó el límite de uso disponible del servicio de inteligencia artificial.");
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
				.isInstanceOf(AnalysisConfigurationException.class)
				.hasMessage("Gemini rechazo la solicitud. Revisa el modelo configurado y el formato enviado.");
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

	private void assertPromptIncludesStableRequirementExtractionRules(String prompt) {
		String normalizedPrompt = prompt.replaceAll("\\s+", " ");

		assertThat(normalizedPrompt)
				.contains("FASE A")
				.contains("exclusivamente desde la oferta")
				.contains("sin usar el CV")
				.contains("FASE B")
				.contains("PRIORIDAD 1")
				.contains("EXPERIENCE_SENIORITY")
				.contains("PRIORIDAD 2")
				.contains("DESIRABLE")
				.contains("PRIORIDAD 3")
				.contains("MANDATORY_TECHNICAL")
				.contains("PRIORIDAD 4")
				.contains("COMPLEMENTARY")
				.contains("clasificalo como MISSING")
				.contains("No uses PARTIAL como resultado de incertidumbre")
				.contains("\"Java o Kotlin\" debe ser un solo requirement")
				.contains("\"Java y Spring Boot\" deben ser dos requirements independientes")
				.contains("no haya requirements duplicados")
				.contains("no debe aparecer como faltante")
				.contains("no debe aparecer como matching");
	}

	private HttpOptions buildHttpOptions(GeminiService geminiService) throws Exception {
		Method buildHttpOptions = GeminiService.class.getDeclaredMethod("buildHttpOptions");
		buildHttpOptions.setAccessible(true);
		return (HttpOptions) buildHttpOptions.invoke(geminiService);
	}

	private GenerateContentConfig buildConfig(GeminiService geminiService) throws Exception {
		Method buildConfig = GeminiService.class.getDeclaredMethod("buildConfig");
		buildConfig.setAccessible(true);
		return (GenerateContentConfig) buildConfig.invoke(geminiService);
	}

	private HttpRetryOptions buildRetryOptions(GeminiService geminiService) throws Exception {
		Method buildRetryOptions = GeminiService.class.getDeclaredMethod("buildRetryOptions");
		buildRetryOptions.setAccessible(true);
		return (HttpRetryOptions) buildRetryOptions.invoke(geminiService);
	}

	private Schema buildResponseSchema(GeminiService geminiService) throws Exception {
		Method buildResponseSchema = GeminiService.class.getDeclaredMethod("buildResponseSchema");
		buildResponseSchema.setAccessible(true);
		return (Schema) buildResponseSchema.invoke(geminiService);
	}

	private GeminiAnalysisResult parseResponse(GeminiService geminiService, String responseText) throws Exception {
		Method parseResponse = GeminiService.class.getDeclaredMethod("parseResponse", String.class);
		parseResponse.setAccessible(true);
		try {
			return (GeminiAnalysisResult) parseResponse.invoke(geminiService, responseText);
		}
		catch (java.lang.reflect.InvocationTargetException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw exception;
		}
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
				  "requirements": [
				    {
				      "name": "Java",
				      "category": "MANDATORY_TECHNICAL",
				      "status": "MATCH",
				      "evidence": "Java aparece en el CV."
				    }
				  ],
				  "matchingSkills": ["Java"],
				  "missingSkills": ["Docker"],
				  "recommendations": ["Practicar Docker", "Destacar experiencia con Java"],
				  "interviewQuestions": ["Pregunta 1", "Pregunta 2", "Pregunta 3"],
				  "jobSearchProfile": {
				    "role": "Java Backend Developer",
				    "seniority": "JUNIOR",
				    "keywords": ["Java", "Spring Boot", "SQL", "REST API"]
				  }
				}
				""";
	}

	private String responseWithRequirement(String name, String category, String status, String evidence) {
		return """
				{
				  "requirements": [
				    {
				      "name": "%s",
				      "category": "%s",
				      "status": "%s",
				      "evidence": "%s"
				    }
				  ],
				  "matchingSkills": [],
				  "missingSkills": [],
				  "recommendations": [],
				  "interviewQuestions": [],
				  "jobSearchProfile": {
				    "role": "Java Backend Developer",
				    "seniority": "JUNIOR",
				    "keywords": ["Java", "Spring Boot", "SQL"]
				  }
				}
				""".formatted(name, category, status, evidence);
	}

	private String responseWithProfile(String profileJson) {
		return """
				{
				  "requirements": [
				    {
				      "name": "Java",
				      "category": "MANDATORY_TECHNICAL",
				      "status": "MATCH",
				      "evidence": "Java aparece en el CV."
				    }
				  ],
				  "matchingSkills": ["Java"],
				  "missingSkills": [],
				  "recommendations": ["Destacar Java", "Practicar entrevistas"],
				  "interviewQuestions": ["Pregunta 1", "Pregunta 2", "Pregunta 3"],
				  "jobSearchProfile": %s
				}
				""".formatted(profileJson);
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
