package com.codercup.jobmatchai.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class MatchScoreCalculatorTest {

	private final MatchScoreCalculator calculator = new MatchScoreCalculator();

	@Test
	void allMatchInSingleCategoryScoresOneHundred() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("SQL", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH)
		));

		assertThat(result.matchPercentage()).isEqualTo(100);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(100, null, null, null));
	}

	@Test
	void allMissingScoresZero() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING)
		));

		assertThat(result.matchPercentage()).isZero();
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(0, null, null, null));
	}

	@Test
	void partialContributesHalf() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.PARTIAL),
				assessment("Kubernetes", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING)
		));

		assertThat(result.matchPercentage()).isEqualTo(50);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(50, null, null, null));
	}

	@Test
	void fourCategoriesUseConfiguredWeights() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("SQL", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Kubernetes", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING),
				assessment("Senior experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.MISSING),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementStatus.MATCH),
				assessment("AWS", RequirementCategory.DESIRABLE, RequirementStatus.MISSING),
				assessment("Git", RequirementCategory.COMPLEMENTARY, RequirementStatus.MATCH)
		));

		assertThat(result.matchPercentage()).isEqualTo(60);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(75, 0, 50, 100));
	}

	@Test
	void normalCriticalityPreservesPreviousScore() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("SQL", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Kubernetes", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MISSING),
				assessment("Seniority", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.NORMAL, RequirementStatus.PARTIAL)
		));

		assertThat(result.matchPercentage()).isEqualTo(69);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(75, 50, null, null));
	}

	@Test
	void criticalMatchDoesNotApplyCap() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.CRITICAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementCriticality.NORMAL, RequirementStatus.MISSING)
		));

		assertThat(result.matchPercentage()).isEqualTo(86);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(100, null, 0, null));
	}

	@Test
	void criticalPartialCapsScoreBelowHighCompatibilityBand() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("SQL", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("2 of 3 years professional experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.PARTIAL),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Git", RequirementCategory.COMPLEMENTARY, RequirementCriticality.NORMAL, RequirementStatus.MATCH)
		));

		assertThat(result.matchPercentage()).isEqualTo(79);
		assertThat(result.basePercentage()).isEqualTo(90);
		assertThat(result.criticalCapApplied()).isTrue();
		assertThat(result.criticalMissingCount()).isZero();
		assertThat(result.criticalPartialCount()).isEqualTo(1);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(100, 50, 100, 100));
	}

	@Test
	void singleCriticalMissingCapsHighScoreAtSixtyNine() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("SQL", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("5+ years professional experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Git", RequirementCategory.COMPLEMENTARY, RequirementCriticality.NORMAL, RequirementStatus.MATCH)
		));

		assertThat(result.matchPercentage()).isEqualTo(69);
		assertThat(result.basePercentage()).isEqualTo(80);
		assertThat(result.criticalCapApplied()).isTrue();
		assertThat(result.criticalMissingCount()).isEqualTo(1);
		assertThat(result.criticalPartialCount()).isZero();
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(100, 0, 100, 100));
	}

	@Test
	void multipleCriticalMissingCapsHighScoreAtFiftyFour() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("5+ years professional experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("Fluent English required", RequirementCategory.COMPLEMENTARY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementCriticality.NORMAL, RequirementStatus.MATCH)
		));

		assertThat(result.matchPercentage()).isEqualTo(54);
		assertThat(result.basePercentage()).isEqualTo(70);
		assertThat(result.criticalCapApplied()).isTrue();
		assertThat(result.criticalMissingCount()).isEqualTo(2);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(100, 0, 100, 0));
	}

	@Test
	void missingDesirableNormalDoesNotApplyCriticalCap() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Docker preferred", RequirementCategory.DESIRABLE, RequirementCriticality.NORMAL, RequirementStatus.MISSING)
		));

		assertThat(result.matchPercentage()).isEqualTo(86);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(100, null, 0, null));
	}

	@Test
	void seniorExperienceMissingWithCompleteTechnologiesDoesNotEndAsHighMatch() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("SQL", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Docker", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("5+ years professional experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING)
		));

		assertThat(result.matchPercentage()).isEqualTo(69);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(100, 0, null, null));
	}

	@Test
	void juniorProfileWithoutCriticalMissingKeepsNormalScore() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("SQL", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.PARTIAL),
				assessment("Junior level", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.NORMAL, RequirementStatus.MATCH)
		));

		assertThat(result.matchPercentage()).isEqualTo(88);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(83, 100, null, null));
	}

	@Test
	void criticalMissingDoesNotRaiseAlreadyLowBaseScore() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MISSING),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MISSING),
				assessment("5+ years professional experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementCriticality.NORMAL, RequirementStatus.MATCH)
		));

		assertThat(result.matchPercentage()).isEqualTo(11);
		assertThat(result.basePercentage()).isEqualTo(11);
		assertThat(result.criticalCapApplied()).isFalse();
		assertThat(result.criticalMissingCount()).isEqualTo(1);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(0, 0, 100, null));
	}

	@Test
	void normalizesOnlyPresentCategories() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("SQL", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Kubernetes", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING),
				assessment("Seniority", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.PARTIAL)
		));

		assertThat(result.matchPercentage()).isEqualTo(69);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(75, 50, null, null));
	}

	@Test
	void singleDesirableCategoryCanReachOneHundred() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementStatus.MATCH)
		));

		assertThat(result.matchPercentage()).isEqualTo(100);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(null, null, 100, null));
	}

	@Test
	void emptyListReturnsZeroWithEmptyBreakdown() {
		MatchScoreResult result = calculator.calculate(List.of());

		assertThat(result.matchPercentage()).isZero();
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(null, null, null, null));
	}

	@Test
	void nullListReturnsZeroWithEmptyBreakdown() {
		MatchScoreResult result = calculator.calculate(null);

		assertThat(result.matchPercentage()).isZero();
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(null, null, null, null));
	}

	@Test
	void sameInputAlwaysProducesSameResult() {
		List<RequirementAssessment> requirements = List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.PARTIAL),
				assessment("Seniority", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.MATCH),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementStatus.MISSING)
		);

		MatchScoreResult firstResult = calculator.calculate(requirements);

		assertThat(calculator.calculate(requirements)).isEqualTo(firstResult);
		assertThat(calculator.calculate(requirements)).isEqualTo(firstResult);
		assertThat(calculator.calculate(requirements)).isEqualTo(firstResult);
	}

	@Test
	void requirementAssessmentValidatesRequiredFields() {
		assertThatThrownBy(() -> new RequirementAssessment(
				null,
				RequirementCategory.MANDATORY_TECHNICAL,
				RequirementStatus.MATCH,
				null
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("name must not be null");
		assertThatThrownBy(() -> new RequirementAssessment(
				"",
				RequirementCategory.MANDATORY_TECHNICAL,
				RequirementStatus.MATCH,
				null
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("name must not be blank");
		assertThatThrownBy(() -> new RequirementAssessment(
				"   ",
				RequirementCategory.MANDATORY_TECHNICAL,
				RequirementStatus.MATCH,
				null
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("name must not be blank");
		assertThatThrownBy(() -> new RequirementAssessment(
				"Java",
				null,
				RequirementStatus.MATCH,
				null
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("category must not be null");
		assertThatThrownBy(() -> new RequirementAssessment(
				"Java",
				RequirementCategory.MANDATORY_TECHNICAL,
				null,
				RequirementStatus.MATCH,
				null
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("criticality must not be null");
		assertThatThrownBy(() -> new RequirementAssessment(
				"Java",
				RequirementCategory.MANDATORY_TECHNICAL,
				null,
				null
		))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("status must not be null");
	}

	@Test
	void singlePartialCategoryScoresFifty() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Seniority", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.PARTIAL)
		));

		assertThat(result.matchPercentage()).isEqualTo(50);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(null, 50, null, null));
	}

	@Test
	void finalScoreUsesOriginalRatioBeforeRoundedBreakdown() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Critical technical skill", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING),
				assessment("Year 1", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.MATCH),
				assessment("Year 2", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.MATCH),
				assessment("Year 3", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.MATCH),
				assessment("Year 4", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.MATCH),
				assessment("Year 5", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.MATCH),
				assessment("Year 6", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.MATCH),
				assessment("Year 7", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.MISSING)
		));

		assertThat(result.matchPercentage()).isEqualTo(21);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(0, 86, null, null));
	}

	private RequirementAssessment assessment(
			String name,
			RequirementCategory category,
			RequirementStatus status
	) {
		return new RequirementAssessment(name, category, status, null);
	}

	private RequirementAssessment assessment(
			String name,
			RequirementCategory category,
			RequirementCriticality criticality,
			RequirementStatus status
	) {
		return new RequirementAssessment(name, category, criticality, status, null);
	}
}
