package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codercup.jobmatchai.dto.AnalysisResponse;
import com.codercup.jobmatchai.dto.internal.GeminiAnalysisResult;
import com.codercup.jobmatchai.dto.internal.GeminiJobSearchProfile;
import com.codercup.jobmatchai.dto.JobSeniority;
import com.codercup.jobmatchai.exception.InvalidCvContentException;
import com.codercup.jobmatchai.scoring.MatchScoreCalculator;
import com.codercup.jobmatchai.scoring.RequirementAssessment;
import com.codercup.jobmatchai.scoring.RequirementCategory;
import com.codercup.jobmatchai.scoring.RequirementCriticality;
import com.codercup.jobmatchai.scoring.RequirementStatus;
import java.util.List;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class AnalysisServiceTest {

	@Test
	void matchPercentageComesFromMatchScoreCalculator() {
		AnalysisService analysisService = new AnalysisService(
				new FakePdfService(),
				new FakeGeminiService(integrationResult()),
				new MatchScoreCalculator()
		);

		AnalysisResponse response = analysisService.analyze(validCvFile(), "Java developer role", null);

		assertThat(response.matchPercentage()).isEqualTo(60);
		assertThat(response.matchingSkills()).containsExactly("Java", "Spring Boot", "SQL", "Docker", "Git");
		assertThat(response.missingSkills()).containsExactly("TypeScript", "5 anos de experiencia", "AWS");
		assertThat(response.recommendations()).containsExactly("Reforzar TypeScript", "Preparar experiencia senior");
		assertThat(response.interviewQuestions()).containsExactly(
				"Como disenarias una API REST?",
				"Como usaste Spring Boot?",
				"Que experiencia tenes con Docker?"
		);
		assertThat(response.requirements()).extracting("name")
				.containsExactly("Java", "Spring Boot", "SQL", "TypeScript", "5 anos de experiencia", "Docker", "AWS", "Git");
		assertThat(response.requirements()).extracting("status")
				.containsExactly("match", "match", "match", "missing", "missing", "match", "missing", "match");
		assertThat(response.breakdown()).isNotNull();
		assertThat(response.criticalMissingRequirements()).isEmpty();
		assertThat(response.experienceGap()).isNotNull();
		assertThat(response.experienceGap().requirement()).isEqualTo("5 anos de experiencia");
		assertThat(response.warnings())
				.containsExactly("La experiencia profesional requerida no esta completamente respaldada por el CV.");
		assertThat(response.jobSearchProfile()).isNotNull();
		assertThat(response.jobSearchProfile().role()).isEqualTo("Java Backend Developer");
		assertThat(response.jobSearchProfile().seniority()).isEqualTo(JobSeniority.JUNIOR);
		assertThat(response.jobSearchProfile().keywords()).containsExactly("Java", "Spring Boot", "SQL", "REST API");
	}

	@Test
	void sameGeminiAnalysisResultAlwaysProducesSameMatchPercentage() {
		AnalysisService analysisService = new AnalysisService(
				new FakePdfService(),
				new FakeGeminiService(integrationResult()),
				new MatchScoreCalculator()
		);

		AnalysisResponse firstResponse = analysisService.analyze(validCvFile(), "Java developer role", null);

		assertThat(analysisService.analyze(validCvFile(), "Java developer role", null).matchPercentage())
				.isEqualTo(firstResponse.matchPercentage());
		assertThat(analysisService.analyze(validCvFile(), "Java developer role", null).matchPercentage())
				.isEqualTo(firstResponse.matchPercentage());
		assertThat(analysisService.analyze(validCvFile(), "Java developer role", null).matchPercentage())
				.isEqualTo(firstResponse.matchPercentage());
	}

	@Test
	void textAndImageFlowsUseMatchScoreCalculator() throws IOException {
		FakeGeminiService geminiService = new FakeGeminiService(integrationResult());
		AnalysisService analysisService = new AnalysisService(
				new FakePdfService(),
				geminiService,
				new MatchScoreCalculator()
		);

		AnalysisResponse textResponse = analysisService.analyze(validCvFile(), "Java developer role", null);
		AnalysisResponse imageResponse = analysisService.analyze(validCvFile(), null, validJobImage());

		assertThat(textResponse.matchPercentage()).isEqualTo(60);
		assertThat(imageResponse.matchPercentage()).isEqualTo(60);
		assertThat(textResponse.jobSearchProfile().role()).isEqualTo("Java Backend Developer");
		assertThat(imageResponse.jobSearchProfile().role()).isEqualTo("Java Backend Developer");
		assertThat(geminiService.textCalls()).isEqualTo(1);
		assertThat(geminiService.imageCalls()).isEqualTo(1);
	}

	@Test
	void sendsDeterministicProfessionalKnowledgeHintsToGemini() throws IOException {
		FakeGeminiService geminiService = new FakeGeminiService(integrationResult());
		AnalysisService analysisService = new AnalysisService(
				new FakePdfService(),
				geminiService,
				new MatchScoreCalculator()
		);

		analysisService.analyze(
				validCvFile(),
				"Oferta administrativa con Java, Microsoft Excel, conciliaciones bancarias y CRM.",
				null
		);
		analysisService.analyze(validCvFile(), null, validJobImage());

		assertThat(geminiService.lastCvKnowledgeHints())
				.contains("Java", "Spring Boot", "SQL", "Docker", "Git", "REST APIs");
		assertThat(geminiService.lastJobKnowledgeHints())
				.contains("Java", "Microsoft Excel", "Bank Reconciliation", "CRM");
		assertThat(geminiService.lastImageCvKnowledgeHints())
				.contains("Java", "Spring Boot", "SQL", "Docker", "Git", "REST APIs");
	}

	@Test
	void validCvIsExtractedValidatedAndSentToGemini() {
		TrackingCvContentValidator validator = new TrackingCvContentValidator(false);
		FakeGeminiService geminiService = new FakeGeminiService(integrationResult());
		AnalysisService analysisService = new AnalysisService(
				new FakePdfService(),
				validator,
				geminiService,
				new MatchScoreCalculator(),
				5000
		);

		AnalysisResponse response = analysisService.analyze(validCvFile(), "Java developer role", null);

		assertThat(validator.validatedText()).contains("Perfil profesional");
		assertThat(geminiService.textCalls()).isEqualTo(1);
		assertThat(response.matchPercentage()).isEqualTo(60);
	}

	@Test
	void responseHasEmptyExplanationFieldsWhenThereAreNoGaps() {
		AnalysisResponse response = analyze(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH)
		));

		assertThat(response.criticalMissingRequirements()).isEmpty();
		assertThat(response.experienceGap()).isNull();
		assertThat(response.warnings()).isEmpty();
	}

	@Test
	void responseIncludesOneCriticalMissingRequirement() {
		AnalysisResponse response = analyze(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment(
						"Fluent English required",
						RequirementCategory.COMPLEMENTARY,
						RequirementCriticality.CRITICAL,
						RequirementStatus.MISSING
				)
		));

		assertThat(response.criticalMissingRequirements()).hasSize(1);
		assertThat(response.criticalMissingRequirements().get(0).requirement()).isEqualTo("Fluent English required");
		assertThat(response.criticalMissingRequirements().get(0).category()).isEqualTo("complementary");
		assertThat(response.warnings())
				.contains(
						"Falta 1 requisito critico de la oferta.",
						"El score esta limitado por requisitos criticos no cumplidos."
				);
	}

	@Test
	void responseIncludesAllCriticalMissingRequirementsWithPluralWarning() {
		AnalysisResponse response = analyze(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment(
						"5+ anos de experiencia profesional",
						RequirementCategory.EXPERIENCE_SENIORITY,
						RequirementCriticality.CRITICAL,
						RequirementStatus.MISSING
				),
				assessment(
						"AWS certification required",
						RequirementCategory.COMPLEMENTARY,
						RequirementCriticality.CRITICAL,
						RequirementStatus.MISSING
				)
		));

		assertThat(response.criticalMissingRequirements()).extracting("requirement")
				.containsExactly("5+ anos de experiencia profesional", "AWS certification required");
		assertThat(response.warnings())
				.contains(
						"Faltan 2 requisitos criticos de la oferta.",
						"El score esta limitado por requisitos criticos no cumplidos."
				);
	}

	@Test
	void criticalPartialDoesNotEnterCriticalMissingRequirementsButWarns() {
		AnalysisResponse response = analyze(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment(
						"Strong React experience is required",
						RequirementCategory.MANDATORY_TECHNICAL,
						RequirementCriticality.CRITICAL,
						RequirementStatus.PARTIAL
				),
				assessment("SQL", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH)
		));

		assertThat(response.criticalMissingRequirements()).isEmpty();
		assertThat(response.warnings())
				.contains("Un requisito critico se cumple parcialmente.");
	}

	@Test
	void experienceCriticalMissingCreatesExperienceGap() {
		AnalysisResponse response = analyze(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment(
						"5+ anos de experiencia profesional",
						RequirementCategory.EXPERIENCE_SENIORITY,
						RequirementCriticality.CRITICAL,
						RequirementStatus.MISSING,
						"El CV no demuestra 5 anos de experiencia profesional."
				)
		));

		assertThat(response.experienceGap()).isNotNull();
		assertThat(response.experienceGap().requirement()).isEqualTo("5+ anos de experiencia profesional");
		assertThat(response.experienceGap().status()).isEqualTo("missing");
		assertThat(response.experienceGap().critical()).isTrue();
		assertThat(response.experienceGap().summary())
				.isEqualTo("El CV no demuestra 5 anos de experiencia profesional.");
	}

	@Test
	void experienceCriticalPartialCreatesExperienceGap() {
		AnalysisResponse response = analyze(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment(
						"3+ anos de experiencia Java",
						RequirementCategory.EXPERIENCE_SENIORITY,
						RequirementCriticality.CRITICAL,
						RequirementStatus.PARTIAL
				)
		));

		assertThat(response.criticalMissingRequirements()).isEmpty();
		assertThat(response.experienceGap()).isNotNull();
		assertThat(response.experienceGap().status()).isEqualTo("partial");
		assertThat(response.experienceGap().critical()).isTrue();
		assertThat(response.warnings())
				.contains("La experiencia profesional requerida no esta completamente respaldada por el CV.");
	}

	@Test
	void experienceGapPriorityChoosesCriticalMissingBeforeOtherExperienceGaps() {
		AnalysisResponse response = analyze(List.of(
				assessment(
						"1 ano de experiencia profesional",
						RequirementCategory.EXPERIENCE_SENIORITY,
						RequirementCriticality.NORMAL,
						RequirementStatus.MISSING
				),
				assessment(
						"5+ anos de experiencia profesional",
						RequirementCategory.EXPERIENCE_SENIORITY,
						RequirementCriticality.CRITICAL,
						RequirementStatus.MISSING
				),
				assessment(
						"Senior Java Developer",
						RequirementCategory.EXPERIENCE_SENIORITY,
						RequirementCriticality.CRITICAL,
						RequirementStatus.PARTIAL
				)
		));

		assertThat(response.experienceGap()).isNotNull();
		assertThat(response.experienceGap().requirement()).isEqualTo("5+ anos de experiencia profesional");
	}

	@Test
	void criticalCapWarningOnlyAppearsWhenScoreWasActuallyLimited() {
		AnalysisResponse response = analyze(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING),
				assessment(
						"5+ anos de experiencia profesional",
						RequirementCategory.EXPERIENCE_SENIORITY,
						RequirementCriticality.CRITICAL,
						RequirementStatus.MISSING
				),
				assessment("Docker preferred", RequirementCategory.DESIRABLE, RequirementStatus.MATCH)
		));

		assertThat(response.matchPercentage()).isEqualTo(11);
		assertThat(response.warnings())
				.contains("Falta 1 requisito critico de la oferta.")
				.doesNotContain("El score esta limitado por requisitos criticos no cumplidos.");
	}

	@Test
	void invalidCvContentStopsBeforeGeminiAndScoring() {
		TrackingCvContentValidator validator = new TrackingCvContentValidator(true);
		FakeGeminiService geminiService = new FakeGeminiService(integrationResult());
		AnalysisService analysisService = new AnalysisService(
				new FakePdfService(),
				validator,
				geminiService,
				new MatchScoreCalculator(),
				5000
		);

		assertThatThrownBy(() -> analysisService.analyze(validCvFile(), "Java developer role", null))
				.isInstanceOf(InvalidCvContentException.class);
		assertThat(validator.validatedText()).contains("Perfil profesional");
		assertThat(geminiService.textCalls()).isZero();
		assertThat(geminiService.imageCalls()).isZero();
	}

	private GeminiAnalysisResult integrationResult() {
		return new GeminiAnalysisResult(
				List.of(
						assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
						assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
						assessment("SQL", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
						assessment("TypeScript", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING),
						assessment("5 anos de experiencia", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.MISSING),
						assessment("Docker", RequirementCategory.DESIRABLE, RequirementStatus.MATCH),
						assessment("AWS", RequirementCategory.DESIRABLE, RequirementStatus.MISSING),
						assessment("Git", RequirementCategory.COMPLEMENTARY, RequirementStatus.MATCH)
				),
				List.of("Java", "Spring Boot", "SQL", "Docker", "Git"),
				List.of("TypeScript", "5 anos de experiencia", "AWS"),
				List.of("Reforzar TypeScript", "Preparar experiencia senior"),
				List.of(
						"Como disenarias una API REST?",
						"Como usaste Spring Boot?",
						"Que experiencia tenes con Docker?"
				),
				new GeminiJobSearchProfile(
						"Java Backend Developer",
						JobSeniority.JUNIOR,
						List.of("Java", "Spring Boot", "SQL", "REST API")
				)
		);
	}

	private AnalysisResponse analyze(List<RequirementAssessment> requirements) {
		AnalysisService analysisService = new AnalysisService(
				new FakePdfService(),
				new FakeGeminiService(resultWithRequirements(requirements)),
				new MatchScoreCalculator()
		);
		return analysisService.analyze(validCvFile(), "Java developer role", null);
	}

	private GeminiAnalysisResult resultWithRequirements(List<RequirementAssessment> requirements) {
		return new GeminiAnalysisResult(
				requirements,
				List.of("Java"),
				List.of("Docker"),
				List.of("Reforzar gaps especificos", "Preparar entrevista tecnica"),
				List.of("Pregunta 1", "Pregunta 2", "Pregunta 3"),
				new GeminiJobSearchProfile(
						"Java Backend Developer",
						JobSeniority.JUNIOR,
						List.of("Java", "Spring Boot", "SQL")
				)
		);
	}

	private RequirementAssessment assessment(
			String name,
			RequirementCategory category,
			RequirementStatus status
	) {
		return new RequirementAssessment(name, category, status, "Evidencia de test");
	}

	private RequirementAssessment assessment(
			String name,
			RequirementCategory category,
			RequirementCriticality criticality,
			RequirementStatus status
	) {
		return assessment(name, category, criticality, status, "Evidencia de test");
	}

	private RequirementAssessment assessment(
			String name,
			RequirementCategory category,
			RequirementCriticality criticality,
			RequirementStatus status,
			String evidence
	) {
		return new RequirementAssessment(name, category, criticality, status, evidence);
	}

	private MockMultipartFile validCvFile() {
		return new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				new byte[] {1}
		);
	}

	private byte[] createPng() throws IOException {
		BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			ImageIO.write(image, "png", outputStream);
			return outputStream.toByteArray();
		}
	}

	private MockMultipartFile validJobImage() throws IOException {
		return new MockMultipartFile(
				"jobImage",
				"job.png",
				"image/png",
				createPng()
		);
	}

	private static class FakePdfService extends PdfService {

		@Override
		public String extractText(MultipartFile file) {
			return """
					Perfil profesional
					Desarrollador backend con experiencia laboral en Java y Spring Boot.
					Educación: Ingeniería en Sistemas.
					Habilidades técnicas: Java, Spring Boot, SQL, Docker y Git.
					Proyectos: API REST para gestión de tareas.
					""";
		}
	}

	private static class TrackingCvContentValidator extends CvContentValidator {

		private final boolean reject;
		private String validatedText;

		TrackingCvContentValidator(boolean reject) {
			this.reject = reject;
		}

		String validatedText() {
			return validatedText;
		}

		@Override
		public void validate(String cvText) {
			validatedText = cvText;
			if (reject) {
				throw new InvalidCvContentException();
			}
		}
	}

	private static class FakeGeminiService extends GeminiService {

		private final GeminiAnalysisResult result;
		private int textCalls;
		private int imageCalls;
		private List<String> lastCvKnowledgeHints = List.of();
		private List<String> lastJobKnowledgeHints = List.of();
		private List<String> lastImageCvKnowledgeHints = List.of();

		FakeGeminiService(GeminiAnalysisResult result) {
			super("test-key", "test-model", 30000, 2, 500);
			this.result = result;
		}

		int textCalls() {
			return textCalls;
		}

		int imageCalls() {
			return imageCalls;
		}

		List<String> lastCvKnowledgeHints() {
			return lastCvKnowledgeHints;
		}

		List<String> lastJobKnowledgeHints() {
			return lastJobKnowledgeHints;
		}

		List<String> lastImageCvKnowledgeHints() {
			return lastImageCvKnowledgeHints;
		}

		@Override
		public GeminiAnalysisResult analyze(String cvText, String jobDescription) {
			textCalls++;
			return result;
		}

		@Override
		public GeminiAnalysisResult analyze(
				String cvText,
				String jobDescription,
				List<String> cvKnowledgeHints,
				List<String> jobKnowledgeHints
		) {
			textCalls++;
			lastCvKnowledgeHints = cvKnowledgeHints;
			lastJobKnowledgeHints = jobKnowledgeHints;
			return result;
		}

		@Override
		public GeminiAnalysisResult analyze(String cvText, MultipartFile jobImage) {
			imageCalls++;
			return result;
		}

		@Override
		public GeminiAnalysisResult analyze(String cvText, MultipartFile jobImage, List<String> cvKnowledgeHints) {
			imageCalls++;
			lastImageCvKnowledgeHints = cvKnowledgeHints;
			return result;
		}
	}
}
