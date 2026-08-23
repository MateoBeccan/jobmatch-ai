package com.codercup.jobmatchai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.codercup.jobmatchai.dto.AnalysisResponse;
import com.codercup.jobmatchai.dto.JobSeniority;
import com.codercup.jobmatchai.dto.RequirementResponse;
import com.codercup.jobmatchai.dto.internal.GeminiAnalysisResult;
import com.codercup.jobmatchai.dto.internal.GeminiJobSearchProfile;
import com.codercup.jobmatchai.scoring.MatchScoreCalculator;
import com.codercup.jobmatchai.scoring.RequirementAssessment;
import com.codercup.jobmatchai.scoring.RequirementCategory;
import com.codercup.jobmatchai.scoring.RequirementStatus;
import com.codercup.jobmatchai.service.AnalysisEvidenceValidator;
import com.codercup.jobmatchai.service.AnalysisService;
import com.codercup.jobmatchai.service.CvContentValidator;
import com.codercup.jobmatchai.service.GeminiService;
import com.codercup.jobmatchai.service.PdfService;
import com.codercup.jobmatchai.service.ProfessionalDomain;
import com.codercup.jobmatchai.service.ProfessionalKnowledgeExtractor;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class GoldenEvaluationSuiteTest {

	private static final int EXPECTED_CASE_COUNT = 30;
	private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}", Pattern.CASE_INSENSITIVE);
	private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?\\d[\\d\\s().-]{6,}\\d");

	private final GoldenResultEvaluator evaluator = new GoldenResultEvaluator();
	private final ProfessionalKnowledgeExtractor knowledgeExtractor = new ProfessionalKnowledgeExtractor();
	private final AnalysisEvidenceValidator evidenceValidator = new AnalysisEvidenceValidator();

	@Test
	void goldenDatasetHasStableIntegrityAndCoverage() {
		List<GoldenAnalysisCase> cases = GoldenCaseFixtures.allCases();

		assertThat(cases).hasSize(EXPECTED_CASE_COUNT);
		assertThat(cases).extracting(GoldenAnalysisCase::id).doesNotHaveDuplicates();
		assertThat(cases).allSatisfy(analysisCase -> {
			assertThat(analysisCase.id()).matches("GOLD-[A-Z]+-\\d{3}");
			assertThat(analysisCase.domain()).isNotNull();
			assertThat(analysisCase.name()).isNotBlank();
			assertThat(analysisCase.purpose()).isNotBlank();
			assertThat(analysisCase.cvText()).isNotBlank();
			assertThat(analysisCase.jobDescription()).isNotBlank();
			assertThat(analysisCase.expectedRequirements()).isNotEmpty();
			assertThat(analysisCase.expectedRequirements()).extracting(GoldenRequirement::name).doesNotHaveDuplicates();
			assertThat(analysisCase.minExpectedScore()).isBetween(0, 100);
			assertThat(analysisCase.maxExpectedScore()).isBetween(0, 100);
			assertThat(analysisCase.minExpectedScore()).isLessThanOrEqualTo(analysisCase.maxExpectedScore());
			assertThat(containsObviousPersonalData(analysisCase.cvText())).isFalse();
			assertThat(containsObviousPersonalData(analysisCase.jobDescription())).isFalse();
		});
		assertThat(countByDomain(cases)).containsExactlyInAnyOrderEntriesOf(Map.of(
				ProfessionalDomain.SOFTWARE_DEVELOPMENT, 8L,
				ProfessionalDomain.DATA_ANALYTICS, 4L,
				ProfessionalDomain.ACCOUNTING_FINANCE, 5L,
				ProfessionalDomain.ADMINISTRATION, 4L,
				ProfessionalDomain.CUSTOMER_SERVICE, 3L,
				ProfessionalDomain.SALES, 2L,
				ProfessionalDomain.HUMAN_RESOURCES, 2L,
				ProfessionalDomain.OPERATIONS, 2L
		));
		assertThat(countByBand(cases)).containsEntry("HIGH", 11L)
				.containsEntry("MEDIUM", 8L)
				.containsEntry("LOW", 11L);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("goldenCases")
	void deterministicPipelineEvaluatesGoldenCaseWithoutNetwork(GoldenAnalysisCase analysisCase) {
		FakeGeminiService geminiService = new FakeGeminiService(analysisCase.modelResult());
		AnalysisService analysisService = new AnalysisService(
				new ScenarioPdfService(analysisCase.cvText()),
				new NoOpCvContentValidator(),
				geminiService,
				new MatchScoreCalculator(),
				new ProfessionalKnowledgeExtractor(),
				new AnalysisEvidenceValidator(),
				5000
		);

		AnalysisResponse response = analysisService.analyze(validCvFile(), analysisCase.jobDescription(), null);

		assertThat(geminiService.textCalls()).isEqualTo(1);
		assertThat(geminiService.imageCalls()).isZero();
		assertThat(response.matchPercentage())
				.as(analysisCase.id() + " score range")
				.isBetween(analysisCase.minExpectedScore(), analysisCase.maxExpectedScore());
		assertThat(response.matchingSkills()).containsAll(analysisCase.expectedMatchingSkills());
		assertThat(response.missingSkills()).containsAll(analysisCase.expectedMissingSkills());
		assertThat(response.criticalMissingRequirements()).extracting("requirement")
				.containsExactlyElementsOf(analysisCase.expectedCriticalRequirements());
		if (analysisCase.expectedExperienceGap() == null) {
			assertThat(response.experienceGap()).isNull();
		}
		else {
			assertThat(response.experienceGap()).isNotNull();
			assertThat(response.experienceGap().requirement()).isEqualTo(analysisCase.expectedExperienceGap());
		}
		analysisCase.expectedRequirements().forEach(expected ->
				assertThat(statusByRequirement(response, expected.name()))
						.as(analysisCase.id() + " " + expected.name())
						.isEqualTo(expected.expectedStatus().name().toLowerCase(java.util.Locale.ROOT)));
	}

	@Test
	void evaluatorBuildsSummaryForValidatedGoldenBaseline() {
		List<GoldenAnalysisCase> cases = GoldenCaseFixtures.allCases();
		List<GoldenEvaluationResult> results = cases.stream()
				.map(analysisCase -> evaluator.evaluate(analysisCase, validatedResult(analysisCase)))
				.toList();
		GoldenEvaluationSummary summary = evaluator.summarize(cases, results);

		assertThat(summary.totalCases()).isEqualTo(EXPECTED_CASE_COUNT);
		assertThat(summary.requirementStatusAccuracy().orElseThrow()).isGreaterThanOrEqualTo(0.8);
		assertThat(summary.matchingSkillPrecision()).hasValue(1.0);
		assertThat(summary.missingSkillPrecision()).hasValue(1.0);
		assertThat(summary.hallucinatedKnownSkillsCount()).isZero();
		assertThat(summary.scoreRangePassRate()).hasValue(1.0);
		assertThat(summary.casesByDomain()).containsEntry(ProfessionalDomain.SOFTWARE_DEVELOPMENT, 8L);
		assertThat(summary.casesByExpectedBand()).containsEntry("HIGH", 11L);
	}

	private GeminiAnalysisResult validatedResult(GoldenAnalysisCase analysisCase) {
		return evidenceValidator.validate(
				analysisCase.modelResult(),
				knowledgeExtractor.extract(analysisCase.cvText()),
				knowledgeExtractor.extract(analysisCase.jobDescription()),
				true
		);
	}

	@Test
	void evaluatorPenalizesUnexpectedKnownSkillsAndRequirements() {
		GoldenAnalysisCase expected = GoldenCaseFixtures.allCases().get(1);
		GeminiAnalysisResult actual = new GeminiAnalysisResult(
				List.of(
						assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
						assessment("Docker", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH)
				),
				List.of("Java", "Docker"),
				List.of("Docker"),
				List.of("Recommendation one", "Recommendation two"),
				List.of("Question one", "Question two", "Question three"),
				new GeminiJobSearchProfile("Golden Fixture Role", JobSeniority.JUNIOR, List.of("Java", "Docker", "AWS"))
		);

		GoldenEvaluationResult result = evaluator.evaluate(expected, actual);

		assertThat(result.requirementStatusAccuracy()).hasValue(1.0);
		assertThat(result.expectedRequirementsNotFound()).contains(
				"AWS",
				"Kubernetes",
				"5+ years professional Java experience"
		);
		assertThat(result.unexpectedRequirements()).containsExactly("Docker");
		assertThat(result.matchingSkillPrecision()).hasValue(0.5);
		assertThat(result.missingSkillPrecision()).hasValue(0.0);
		assertThat(result.hallucinatedKnownSkillsCount()).isEqualTo(2);
		assertThat(result.scoreWithinExpectedRange()).isFalse();
	}

	static Stream<GoldenAnalysisCase> goldenCases() {
		return GoldenCaseFixtures.allCases().stream();
	}

	private boolean containsObviousPersonalData(String value) {
		return EMAIL_PATTERN.matcher(value).find() || PHONE_PATTERN.matcher(value).find();
	}

	private Map<ProfessionalDomain, Long> countByDomain(List<GoldenAnalysisCase> cases) {
		return cases.stream().collect(Collectors.groupingBy(
				GoldenAnalysisCase::domain,
				Collectors.counting()
		));
	}

	private Map<String, Long> countByBand(List<GoldenAnalysisCase> cases) {
		return cases.stream().collect(Collectors.groupingBy(
				this::expectedBand,
				Collectors.counting()
		));
	}

	private String expectedBand(GoldenAnalysisCase analysisCase) {
		if (analysisCase.minExpectedScore() >= 80) {
			return "HIGH";
		}
		if (analysisCase.maxExpectedScore() < 70) {
			return "LOW";
		}
		return "MEDIUM";
	}

	private String statusByRequirement(AnalysisResponse response, String requirement) {
		return response.requirements().stream()
				.filter(item -> item.name().equals(requirement))
				.map(RequirementResponse::status)
				.findFirst()
				.orElseThrow();
	}

	private RequirementAssessment assessment(
			String name,
			RequirementCategory category,
			RequirementStatus status
	) {
		return new RequirementAssessment(name, category, status, "Evaluator fixture evidence.");
	}

	private MockMultipartFile validCvFile() {
		return new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				new byte[] {1}
		);
	}

	private static class ScenarioPdfService extends PdfService {

		private final String cvText;

		ScenarioPdfService(String cvText) {
			this.cvText = cvText;
		}

		@Override
		public String extractText(MultipartFile file) {
			return """
					Professional profile
					%s
					Education: synthetic training summary.
					This fixture is synthetic and deterministic for golden evaluation.
					""".formatted(cvText);
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
		public GeminiAnalysisResult analyze(
				String cvText,
				String jobDescription,
				List<String> cvKnowledgeHints,
				List<String> jobKnowledgeHints
		) {
			textCalls++;
			return result;
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

		@Override
		public GeminiAnalysisResult analyze(String cvText, MultipartFile jobImage, List<String> cvKnowledgeHints) {
			imageCalls++;
			return result;
		}
	}

	private static class NoOpCvContentValidator extends CvContentValidator {

		@Override
		public void validate(String cvText) {
		}
	}
}
