package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.codercup.jobmatchai.dto.RequirementExplainabilityResponse;
import com.codercup.jobmatchai.dto.ScoreExplanationResponse;
import com.codercup.jobmatchai.dto.internal.GeminiAnalysisResult;
import com.codercup.jobmatchai.dto.internal.GeminiJobSearchProfile;
import com.codercup.jobmatchai.dto.JobSeniority;
import com.codercup.jobmatchai.scoring.MatchScoreCalculator;
import com.codercup.jobmatchai.scoring.RequirementAssessment;
import com.codercup.jobmatchai.scoring.RequirementCategory;
import com.codercup.jobmatchai.scoring.RequirementCriticality;
import com.codercup.jobmatchai.scoring.RequirementStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnalysisExplainabilityServiceTest {

	private final AnalysisExplainabilityService explainabilityService = new AnalysisExplainabilityService();
	private final MatchScoreCalculator scoreCalculator = new MatchScoreCalculator();
	private final ProfessionalKnowledgeExtractor extractor = new ProfessionalKnowledgeExtractor();

	@Test
	void knownRequirementChangedFromMatchToMissingUsesFinalSafeEvidence() {
		GeminiAnalysisResult original = result(List.of(
				assessment("Docker", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH,
						"El CV demuestra Docker.")
		));
		GeminiAnalysisResult validated = result(List.of(
				assessment("Docker", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING,
						"El CV demuestra Docker.")
		));

		AnalysisExplainabilityService.RequirementExplanation explanation = explainabilityService.explainRequirements(
				original,
				validated,
				extractor.extract("Java SQL"),
				extractor.extract("Java Docker"),
				true
		).get(0);
		RequirementExplainabilityResponse details = explanation.explainability();

		assertThat(explanation.requirement().status()).isEqualTo(RequirementStatus.MISSING);
		assertThat(explanation.finalEvidence()).isEqualTo(
				"No se detecto una mencion de Docker en el texto extraido del CV.");
		assertThat(explanation.finalEvidence()).doesNotContain("demuestra Docker");
		assertThat(details.evidenceBasis()).isEqualTo("deterministic_catalog");
		assertThat(details.statusAdjusted()).isTrue();
		assertThat(details.originalStatus()).isEqualTo("match");
		assertThat(details.cvCatalogEvidenceDetected()).isFalse();
		assertThat(details.jobCatalogEvidenceDetected()).isTrue();
	}

	@Test
	void knownRequirementChangedFromMissingToPartialExplainsTextualEvidenceWithoutOverclaiming() {
		GeminiAnalysisResult original = result(List.of(
				assessment("Microsoft Excel", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING,
						"Excel no esta presente.")
		));
		GeminiAnalysisResult validated = result(List.of(
				assessment("Microsoft Excel", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.PARTIAL,
						"Excel no esta presente.")
		));

		AnalysisExplainabilityService.RequirementExplanation explanation = explainabilityService.explainRequirements(
				original,
				validated,
				extractor.extract("Analista con Microsoft Excel y reportes."),
				extractor.extract("Oferta con Microsoft Excel."),
				true
		).get(0);
		RequirementExplainabilityResponse details = explanation.explainability();

		assertThat(explanation.requirement().status()).isEqualTo(RequirementStatus.PARTIAL);
		assertThat(explanation.finalEvidence()).contains("Se detecto Microsoft Excel en el CV");
		assertThat(explanation.finalEvidence()).doesNotContain("no esta presente");
		assertThat(details.statusAdjusted()).isTrue();
		assertThat(details.originalStatus()).isEqualTo("missing");
		assertThat(details.cvCatalogEvidenceDetected()).isTrue();
		assertThat(details.jobCatalogEvidenceDetected()).isTrue();
	}

	@Test
	void knownMatchWithoutCorrectionKeepsAdjustmentTraceEmpty() {
		GeminiAnalysisResult original = result(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH)
		));

		AnalysisExplainabilityService.RequirementExplanation explanation = explainabilityService.explainRequirements(
				original,
				original,
				extractor.extract("Java SQL"),
				extractor.extract("Java developer"),
				true
		).get(0);

		assertThat(explanation.explainability().evidenceBasis()).isEqualTo("deterministic_catalog");
		assertThat(explanation.explainability().statusAdjusted()).isFalse();
		assertThat(explanation.explainability().originalStatus()).isNull();
		assertThat(explanation.explainability().cvCatalogEvidenceDetected()).isTrue();
		assertThat(explanation.finalEvidence()).contains("Java").contains("MATCH");
	}

	@Test
	void knownPartialUsesPrudentLanguage() {
		GeminiAnalysisResult result = result(List.of(
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.PARTIAL)
		));

		AnalysisExplainabilityService.RequirementExplanation explanation = explainabilityService.explainRequirements(
				result,
				result,
				extractor.extract("Proyecto con Spring Boot."),
				extractor.extract("Oferta Spring Boot."),
				true
		).get(0);

		assertThat(explanation.explainability().statusAdjusted()).isFalse();
		assertThat(explanation.finalEvidence())
				.contains("Spring Boot")
				.contains("no quedo suficientemente respaldado como cumplimiento completo");
	}

	@Test
	void complexUnknownAndExperienceRequirementsDoNotExposeCatalogBooleans() {
		GeminiAnalysisResult result = result(List.of(
				assessment("Java or Kotlin", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("3+ years professional Java experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.MISSING),
				assessment("Oracle NetSuite", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING),
				assessment("Advanced Excel", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.PARTIAL)
		));

		List<AnalysisExplainabilityService.RequirementExplanation> explanations = explainabilityService.explainRequirements(
				result,
				result,
				extractor.extract("Java and Microsoft Excel."),
				extractor.extract("Java or Kotlin, 3+ years professional Java experience, Oracle NetSuite, Advanced Excel."),
				true
		);

		assertThat(explanations.get(0).explainability().evidenceBasis()).isEqualTo("semantic_analysis");
		assertThat(explanations.get(0).explainability().cvCatalogEvidenceDetected()).isNull();
		assertThat(explanations.get(1).explainability().evidenceBasis()).isEqualTo("experience_semantic_analysis");
		assertThat(explanations.get(1).explainability().cvCatalogEvidenceDetected()).isNull();
		assertThat(explanations.get(2).explainability().evidenceBasis()).isEqualTo("semantic_analysis");
		assertThat(explanations.get(2).explainability().cvCatalogEvidenceDetected()).isNull();
		assertThat(explanations.get(3).explainability().evidenceBasis()).isEqualTo("semantic_analysis");
		assertThat(explanations.get(3).explainability().cvCatalogEvidenceDetected()).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"Advanced Excel",
			"Excel avanzado",
			"Java or Kotlin",
			"Java and Spring Boot",
			"PostgreSQL or equivalent relational database",
			"Java 17+",
			"3+ years Docker"
	})
	void complexRequirementsUseSemanticAnalysis(String requirementName) {
		GeminiAnalysisResult result = result(List.of(
				assessment(requirementName, RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.PARTIAL)
		));

		AnalysisExplainabilityService.RequirementExplanation explanation = explainabilityService.explainRequirements(
				result,
				result,
				extractor.extract("Java Spring Boot PostgreSQL Docker Microsoft Excel."),
				extractor.extract(requirementName),
				true
		).get(0);

		assertThat(explanation.explainability().evidenceBasis()).isEqualTo("semantic_analysis");
		assertThat(explanation.explainability().cvCatalogEvidenceDetected()).isNull();
		assertThat(explanation.explainability().jobCatalogEvidenceDetected()).isNull();
	}

	@Test
	void simpleKnownAliasesStillUseDeterministicCatalog() {
		GeminiAnalysisResult result = result(List.of(
				assessment("MS Excel", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Docker", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH)
		));

		List<AnalysisExplainabilityService.RequirementExplanation> explanations = explainabilityService.explainRequirements(
				result,
				result,
				extractor.extract("Microsoft Excel Java Docker."),
				extractor.extract("MS Excel Java Docker."),
				true
		);

		assertThat(explanations)
				.extracting(explanation -> explanation.explainability().evidenceBasis())
				.containsExactly("deterministic_catalog", "deterministic_catalog", "deterministic_catalog");
		assertThat(explanations)
				.extracting(explanation -> explanation.explainability().cvCatalogEvidenceDetected())
				.containsExactly(true, true, true);
	}

	@Test
	void advancedExcelExplainsProficiencyAsSemanticRequirement() {
		GeminiAnalysisResult result = result(List.of(
				assessment("Advanced Excel", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.PARTIAL)
		));

		AnalysisExplainabilityService.RequirementExplanation explanation = explainabilityService.explainRequirements(
				result,
				result,
				extractor.extract("Microsoft Excel e Invoicing"),
				extractor.extract("Advanced Excel and invoicing"),
				true
		).get(0);

		assertThat(explanation.requirement().status()).isEqualTo(RequirementStatus.PARTIAL);
		assertThat(explanation.explainability().evidenceBasis()).isEqualTo("semantic_analysis");
		assertThat(explanation.explainability().cvCatalogEvidenceDetected()).isNull();
		assertThat(explanation.explainability().jobCatalogEvidenceDetected()).isNull();
		assertThat(explanation.explainability().statusAdjusted()).isFalse();
		assertThat(explanation.explainability().originalStatus()).isNull();
		assertThat(explanation.finalEvidence())
				.contains("Microsoft Excel")
				.contains("interpretacion semantica")
				.doesNotContain("validacion local corrigio");
	}

	@Test
	void experienceRequirementUsesExperienceSemanticAnalysis() {
		GeminiAnalysisResult result = result(List.of(
				assessment("5+ years professional Java experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.MISSING)
		));

		AnalysisExplainabilityService.RequirementExplanation explanation = explainabilityService.explainRequirements(
				result,
				result,
				extractor.extract("Java."),
				extractor.extract("5+ years professional Java experience."),
				true
		).get(0);

		assertThat(explanation.explainability().evidenceBasis()).isEqualTo("experience_semantic_analysis");
		assertThat(explanation.explainability().cvCatalogEvidenceDetected()).isNull();
		assertThat(explanation.explainability().jobCatalogEvidenceDetected()).isNull();
	}

	@Test
	void imageFlowLeavesJobCatalogEvidenceUnavailable() {
		GeminiAnalysisResult result = result(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH)
		));

		AnalysisExplainabilityService.RequirementExplanation explanation = explainabilityService.explainRequirements(
				result,
				result,
				extractor.extract("Java SQL"),
				List.of(),
				false
		).get(0);

		assertThat(explanation.explainability().cvCatalogEvidenceDetected()).isTrue();
		assertThat(explanation.explainability().jobCatalogEvidenceDetected()).isNull();
	}

	@Test
	void scoreExplanationUsesMatchScoreResultForCapReasons() {
		ScoreExplanationResponse noCap = explainabilityService.explainScore(scoreCalculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH)
		)));
		ScoreExplanationResponse criticalPartial = explainabilityService.explainScore(scoreCalculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("SQL", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.PARTIAL),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementStatus.MATCH),
				assessment("Git", RequirementCategory.COMPLEMENTARY, RequirementStatus.MATCH)
		)));
		ScoreExplanationResponse singleMissing = explainabilityService.explainScore(scoreCalculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementStatus.MATCH),
				assessment("Git", RequirementCategory.COMPLEMENTARY, RequirementStatus.MATCH)
		)));
		ScoreExplanationResponse multipleMissing = explainabilityService.explainScore(scoreCalculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("English", RequirementCategory.COMPLEMENTARY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementStatus.MATCH)
		)));

		assertThat(noCap.capReason()).isEqualTo("none");
		assertThat(noCap.basePercentage()).isEqualTo(noCap.finalPercentage());
		assertThat(criticalPartial.basePercentage()).isEqualTo(90);
		assertThat(criticalPartial.finalPercentage()).isEqualTo(79);
		assertThat(criticalPartial.capReason()).isEqualTo("critical_partial");
		assertThat(singleMissing.basePercentage()).isEqualTo(80);
		assertThat(singleMissing.finalPercentage()).isEqualTo(69);
		assertThat(singleMissing.capReason()).isEqualTo("single_critical_missing");
		assertThat(multipleMissing.finalPercentage()).isEqualTo(54);
		assertThat(multipleMissing.capReason()).isEqualTo("multiple_critical_missing");
	}

	@Test
	void scoreExplanationKeepsNoneWhenCriticalMissingDoesNotCapAlreadyLowScore() {
		ScoreExplanationResponse explanation = explainabilityService.explainScore(scoreCalculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("AWS", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING),
				assessment("Kubernetes", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING),
				assessment(
						"5+ years professional Java experience",
						RequirementCategory.EXPERIENCE_SENIORITY,
						RequirementCriticality.CRITICAL,
						RequirementStatus.MISSING
				)
		)));

		assertThat(explanation.basePercentage()).isEqualTo(25);
		assertThat(explanation.finalPercentage()).isEqualTo(25);
		assertThat(explanation.criticalMissingCount()).isEqualTo(1);
		assertThat(explanation.criticalCapApplied()).isFalse();
		assertThat(explanation.capReason()).isEqualTo("none");
		assertThat(explanation.summary()).isEqualTo("El porcentaje final coincide con el score base.");
	}

	@Test
	void scoreExplanationOrdersMixedCriticalReasonsByAppliedScoringPolicy() {
		ScoreExplanationResponse criticalPartial = explainabilityService.explainScore(scoreCalculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.PARTIAL),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementStatus.MATCH),
				assessment("Git", RequirementCategory.COMPLEMENTARY, RequirementStatus.MATCH)
		)));
		ScoreExplanationResponse singleMissing = explainabilityService.explainScore(scoreCalculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementStatus.MATCH),
				assessment("Git", RequirementCategory.COMPLEMENTARY, RequirementStatus.MATCH)
		)));
		ScoreExplanationResponse multipleMissing = explainabilityService.explainScore(scoreCalculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("English", RequirementCategory.COMPLEMENTARY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementStatus.MATCH)
		)));
		ScoreExplanationResponse missingAndPartial = explainabilityService.explainScore(scoreCalculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("English", RequirementCategory.COMPLEMENTARY, RequirementCriticality.CRITICAL, RequirementStatus.PARTIAL),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementStatus.MATCH)
		)));
		ScoreExplanationResponse multipleMissingAndPartial = explainabilityService.explainScore(scoreCalculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("English", RequirementCategory.COMPLEMENTARY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("Security", RequirementCategory.DESIRABLE, RequirementCriticality.CRITICAL, RequirementStatus.PARTIAL)
		)));

		assertThat(criticalPartial.capReason()).isEqualTo("critical_partial");
		assertThat(singleMissing.capReason()).isEqualTo("single_critical_missing");
		assertThat(multipleMissing.capReason()).isEqualTo("multiple_critical_missing");
		assertThat(missingAndPartial.capReason()).isEqualTo("single_critical_missing");
		assertThat(multipleMissingAndPartial.capReason()).isEqualTo("multiple_critical_missing");
	}

	@Test
	void duplicateRequirementIdentityUsesIndexCorrelation() {
		GeminiAnalysisResult original = result(List.of(
				assessment("Docker", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Docker", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.PARTIAL)
		));
		GeminiAnalysisResult validated = result(List.of(
				assessment("Docker", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING),
				assessment("Docker", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING)
		));

		List<AnalysisExplainabilityService.RequirementExplanation> explanations = explainabilityService.explainRequirements(
				original,
				validated,
				extractor.extract("Java."),
				extractor.extract("Docker."),
				true
		);

		assertThat(explanations.get(0).explainability().statusAdjusted()).isTrue();
		assertThat(explanations.get(0).explainability().originalStatus()).isEqualTo("match");
		assertThat(explanations.get(1).explainability().statusAdjusted()).isTrue();
		assertThat(explanations.get(1).explainability().originalStatus()).isEqualTo("partial");
	}

	@Test
	void mismatchedIdentityDoesNotBorrowOriginalStatusFromAnotherOccurrence() {
		GeminiAnalysisResult original = result(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Docker", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.PARTIAL)
		));
		GeminiAnalysisResult validated = result(List.of(
				assessment("Docker", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING)
		));

		AnalysisExplainabilityService.RequirementExplanation explanation = explainabilityService.explainRequirements(
				original,
				validated,
				extractor.extract("Java."),
				extractor.extract("Docker."),
				true
		).get(0);

		assertThat(explanation.explainability().statusAdjusted()).isFalse();
		assertThat(explanation.explainability().originalStatus()).isNull();
	}

	@Test
	void cardinalityMismatchDoesNotInventCorrectionTrace() {
		GeminiAnalysisResult original = result(List.of(
				assessment("Docker", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH)
		));
		GeminiAnalysisResult validated = result(List.of(
				assessment("Docker", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING),
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH)
		));

		List<AnalysisExplainabilityService.RequirementExplanation> explanations = explainabilityService.explainRequirements(
				original,
				validated,
				extractor.extract("Java."),
				extractor.extract("Docker Java."),
				true
		);

		assertThat(explanations.get(0).explainability().statusAdjusted()).isTrue();
		assertThat(explanations.get(0).explainability().originalStatus()).isEqualTo("match");
		assertThat(explanations.get(1).explainability().statusAdjusted()).isFalse();
		assertThat(explanations.get(1).explainability().originalStatus()).isNull();
		assertThat(explanations.get(1).explainability().evidenceBasis()).isEqualTo("deterministic_catalog");
	}

	private GeminiAnalysisResult result(List<RequirementAssessment> requirements) {
		return new GeminiAnalysisResult(
				requirements,
				List.of("Java"),
				List.of("Docker"),
				List.of("Recommendation one", "Recommendation two"),
				List.of("Question one", "Question two", "Question three"),
				new GeminiJobSearchProfile("Java Backend Developer", JobSeniority.JUNIOR, List.of("Java", "SQL", "Git"))
		);
	}

	private RequirementAssessment assessment(
			String name,
			RequirementCategory category,
			RequirementStatus status
	) {
		return new RequirementAssessment(name, category, RequirementCriticality.NORMAL, status, "Gemini evidence.");
	}

	private RequirementAssessment assessment(
			String name,
			RequirementCategory category,
			RequirementStatus status,
			String evidence
	) {
		return new RequirementAssessment(name, category, RequirementCriticality.NORMAL, status, evidence);
	}

	private RequirementAssessment assessment(
			String name,
			RequirementCategory category,
			RequirementCriticality criticality,
			RequirementStatus status
	) {
		return new RequirementAssessment(name, category, criticality, status, "Gemini evidence.");
	}
}
