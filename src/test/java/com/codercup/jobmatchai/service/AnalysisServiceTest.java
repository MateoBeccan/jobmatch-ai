package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.codercup.jobmatchai.dto.AnalysisResponse;
import com.codercup.jobmatchai.dto.internal.GeminiAnalysisResult;
import com.codercup.jobmatchai.scoring.MatchScoreCalculator;
import com.codercup.jobmatchai.scoring.RequirementAssessment;
import com.codercup.jobmatchai.scoring.RequirementCategory;
import com.codercup.jobmatchai.scoring.RequirementStatus;
import java.util.List;
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
	void textAndImageFlowsUseMatchScoreCalculator() {
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
		assertThat(geminiService.textCalls()).isEqualTo(1);
		assertThat(geminiService.imageCalls()).isEqualTo(1);
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

	private MockMultipartFile validCvFile() {
		return new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				new byte[] {1}
		);
	}

	private MockMultipartFile validJobImage() {
		return new MockMultipartFile(
				"jobImage",
				"job.png",
				"image/png",
				new byte[] {1}
		);
	}

	private static class FakePdfService extends PdfService {

		@Override
		public String extractText(MultipartFile file) {
			return "CV con Java, Spring Boot, SQL, Docker y Git";
		}
	}

	private static class FakeGeminiService extends GeminiService {

		private final GeminiAnalysisResult result;
		private int textCalls;
		private int imageCalls;

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

		@Override
		public GeminiAnalysisResult analyze(String cvText, String jobDescription) {
			textCalls++;
			return result;
		}

		@Override
		public GeminiAnalysisResult analyze(String cvText, MultipartFile jobImage) {
			imageCalls++;
			return result;
		}
	}
}
