package com.codercup.jobmatchai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.codercup.jobmatchai.scoring.MatchScoreCalculator;
import com.codercup.jobmatchai.scoring.MatchScoreResult;
import com.codercup.jobmatchai.scoring.RequirementAssessment;
import com.codercup.jobmatchai.scoring.RequirementCategory;
import com.codercup.jobmatchai.scoring.RequirementCriticality;
import com.codercup.jobmatchai.scoring.RequirementStatus;
import com.codercup.jobmatchai.scoring.ScoreBreakdown;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScoringCalibrationTest {

	private static final int PREVIOUS_CRITICAL_PARTIAL_CAP = 85;
	private static final int CURRENT_CRITICAL_PARTIAL_CAP = 79;
	private static final int SINGLE_CRITICAL_MISSING_CAP = 69;
	private static final int MULTIPLE_CRITICAL_MISSING_CAP = 54;

	private final MatchScoreCalculator calculator = new MatchScoreCalculator();

	@Test
	void goldenCasesProduceDocumentedScoringBaseline() {
		List<ScoringCalibrationSnapshot> snapshots = goldenSnapshots();

		assertThat(snapshots).hasSize(30);
		assertThat(scoresByCase(snapshots)).containsAllEntriesOf(Map.ofEntries(
				Map.entry("GOLD-IT-001", 100),
				Map.entry("GOLD-IT-002", 25),
				Map.entry("GOLD-IT-003", 100),
				Map.entry("GOLD-IT-004", 50),
				Map.entry("GOLD-IT-005", 75),
				Map.entry("GOLD-IT-006", 69),
				Map.entry("GOLD-IT-007", 100),
				Map.entry("GOLD-IT-008", 50),
				Map.entry("GOLD-DATA-001", 100),
				Map.entry("GOLD-DATA-002", 43),
				Map.entry("GOLD-DATA-003", 25),
				Map.entry("GOLD-DATA-004", 100),
				Map.entry("GOLD-ACC-001", 100),
				Map.entry("GOLD-ACC-002", 33),
				Map.entry("GOLD-ACC-003", 14),
				Map.entry("GOLD-ACC-004", 69),
				Map.entry("GOLD-ACC-005", 75),
				Map.entry("GOLD-ADM-001", 100),
				Map.entry("GOLD-ADM-002", 86),
				Map.entry("GOLD-ADM-003", 50),
				Map.entry("GOLD-ADM-004", 86),
				Map.entry("GOLD-CS-001", 100),
				Map.entry("GOLD-CS-002", 50),
				Map.entry("GOLD-CS-003", 86),
				Map.entry("GOLD-SALES-001", 100),
				Map.entry("GOLD-SALES-002", 0),
				Map.entry("GOLD-HR-001", 100),
				Map.entry("GOLD-HR-002", 0),
				Map.entry("GOLD-OPS-001", 100),
				Map.entry("GOLD-OPS-002", 43)
		));
	}

	@Test
	void goldenScoresStayInsideExpectedRanges() {
		List<ScoringCalibrationSnapshot> outsideRange = goldenSnapshots().stream()
				.filter(snapshot -> snapshot.finalScore() < snapshot.minExpectedScore()
						|| snapshot.finalScore() > snapshot.maxExpectedScore())
				.toList();

		assertThat(outsideRange).isEmpty();
	}

	@Test
	void goldenScoreDistributionIsExplicitlyTracked() {
		List<ScoringCalibrationSnapshot> snapshots = goldenSnapshots();

		assertThat(countExpectedBands(snapshots)).containsAllEntriesOf(Map.of(
				ScoreBand.HIGH, 11,
				ScoreBand.MEDIUM, 8,
				ScoreBand.LOW, 11
		));
		assertThat(countActualBands(snapshots)).containsAllEntriesOf(Map.of(
				ScoreBand.HIGH, 14,
				ScoreBand.MEDIUM, 2,
				ScoreBand.LOW, 14
		));
	}

	@Test
	void criticalPartialCapChangeDoesNotMoveCurrentGoldenCases() {
		List<ScoringCalibrationSnapshot> changedCases = goldenSnapshots().stream()
				.filter(snapshot -> snapshot.finalScore() != applyPolicyWithCriticalPartialCap(
						snapshot.baseScore(),
						snapshot.criticalMissingCount(),
						snapshot.criticalPartialCount(),
						PREVIOUS_CRITICAL_PARTIAL_CAP))
				.toList();

		assertThat(changedCases).isEmpty();
	}

	@Test
	void criticalPartialCapKeepsScoreBelowHighCompatibilityBand() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("SQL", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("2 of 3 years professional experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.PARTIAL),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Git", RequirementCategory.COMPLEMENTARY, RequirementCriticality.NORMAL, RequirementStatus.MATCH)
		));

		assertThat(result.basePercentage()).isEqualTo(90);
		assertThat(result.matchPercentage()).isEqualTo(CURRENT_CRITICAL_PARTIAL_CAP);
		assertThat(applyPolicyWithCriticalPartialCap(
				result.basePercentage(),
				result.criticalMissingCount(),
				result.criticalPartialCount(),
				PREVIOUS_CRITICAL_PARTIAL_CAP)).isEqualTo(85);
		assertThat(result.criticalCapApplied()).isTrue();
	}

	@Test
	void criticalCapsRemainOrderedBySeverity() {
		MatchScoreResult normal = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Seniority", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Git", RequirementCategory.COMPLEMENTARY, RequirementCriticality.NORMAL, RequirementStatus.MATCH)
		));
		MatchScoreResult criticalPartial = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Seniority", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.PARTIAL),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Git", RequirementCategory.COMPLEMENTARY, RequirementCriticality.NORMAL, RequirementStatus.MATCH)
		));
		MatchScoreResult singleCriticalMissing = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Seniority", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Git", RequirementCategory.COMPLEMENTARY, RequirementCriticality.NORMAL, RequirementStatus.MATCH)
		));
		MatchScoreResult multipleCriticalMissing = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementCriticality.NORMAL, RequirementStatus.MATCH),
				assessment("Seniority", RequirementCategory.EXPERIENCE_SENIORITY, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementCriticality.CRITICAL, RequirementStatus.MISSING),
				assessment("Git", RequirementCategory.COMPLEMENTARY, RequirementCriticality.NORMAL, RequirementStatus.MATCH)
		));

		assertThat(normal.matchPercentage()).isGreaterThan(criticalPartial.matchPercentage());
		assertThat(criticalPartial.matchPercentage()).isEqualTo(CURRENT_CRITICAL_PARTIAL_CAP);
		assertThat(singleCriticalMissing.matchPercentage()).isEqualTo(SINGLE_CRITICAL_MISSING_CAP);
		assertThat(multipleCriticalMissing.matchPercentage()).isEqualTo(MULTIPLE_CRITICAL_MISSING_CAP);
	}

	@Test
	void optionalRequirementsDoNotDominateCompleteMandatoryCoverage() {
		MatchScoreResult missingDesirable = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementStatus.MISSING)
		));
		MatchScoreResult missingComplementary = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Git", RequirementCategory.COMPLEMENTARY, RequirementStatus.MISSING)
		));
		MatchScoreResult allMatched = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementStatus.MATCH)
		));

		assertThat(missingDesirable.matchPercentage()).isEqualTo(86);
		assertThat(missingComplementary.matchPercentage()).isEqualTo(86);
		assertThat(allMatched.matchPercentage()).isEqualTo(100);
	}

	@Test
	void missingMandatoryPenalizesMoreThanMissingDesirableInComparablePattern() {
		MatchScoreResult missingMandatory = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING)
		));
		MatchScoreResult missingDesirable = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementStatus.MISSING)
		));

		assertThat(missingMandatory.matchPercentage()).isEqualTo(50);
		assertThat(missingDesirable.matchPercentage()).isEqualTo(86);
		assertThat(missingMandatory.matchPercentage()).isLessThan(missingDesirable.matchPercentage());
	}

	@Test
	void absentCategoriesAreNotPenalized() {
		MatchScoreResult result = calculator.calculate(List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH)
		));

		assertThat(result.matchPercentage()).isEqualTo(100);
		assertThat(result.breakdown()).isEqualTo(new ScoreBreakdown(100, null, null, null));
	}

	@Test
	void sameRequirementPatternIsDomainNeutral() {
		List<RequirementAssessment> softwareRequirements = List.of(
				assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING),
				assessment("Professional experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.PARTIAL),
				assessment("Docker", RequirementCategory.DESIRABLE, RequirementStatus.MATCH)
		);
		List<RequirementAssessment> accountingRequirements = List.of(
				assessment("Microsoft Excel", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
				assessment("SAP", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MISSING),
				assessment("Payroll experience", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.PARTIAL),
				assessment("QuickBooks", RequirementCategory.DESIRABLE, RequirementStatus.MATCH)
		);

		assertThat(calculator.calculate(accountingRequirements))
				.isEqualTo(calculator.calculate(softwareRequirements));
	}

	private List<ScoringCalibrationSnapshot> goldenSnapshots() {
		return GoldenCaseFixtures.allCases().stream()
				.map(analysisCase -> ScoringCalibrationSnapshot.from(
						analysisCase,
						calculator.calculate(analysisCase.expectedRequirements().stream()
								.map(GoldenRequirement::toAssessment)
								.toList())))
				.toList();
	}

	private Map<String, Integer> scoresByCase(List<ScoringCalibrationSnapshot> snapshots) {
		Map<String, Integer> scores = new java.util.LinkedHashMap<>();
		snapshots.forEach(snapshot -> scores.put(snapshot.id(), snapshot.finalScore()));
		return scores;
	}

	private Map<ScoreBand, Integer> countExpectedBands(List<ScoringCalibrationSnapshot> snapshots) {
		Map<ScoreBand, Integer> counts = emptyBandCounts();
		snapshots.forEach(snapshot -> counts.compute(
				ScoreBand.fromExpectedRange(snapshot.minExpectedScore(), snapshot.maxExpectedScore()),
				(ignored, count) -> count + 1));
		return counts;
	}

	private Map<ScoreBand, Integer> countActualBands(List<ScoringCalibrationSnapshot> snapshots) {
		Map<ScoreBand, Integer> counts = emptyBandCounts();
		snapshots.forEach(snapshot -> counts.compute(
				ScoreBand.fromScore(snapshot.finalScore()),
				(ignored, count) -> count + 1));
		return counts;
	}

	private Map<ScoreBand, Integer> emptyBandCounts() {
		Map<ScoreBand, Integer> counts = new EnumMap<>(ScoreBand.class);
		for (ScoreBand band : ScoreBand.values()) {
			counts.put(band, 0);
		}
		return counts;
	}

	private int applyPolicyWithCriticalPartialCap(
			int baseScore,
			int criticalMissingCount,
			int criticalPartialCount,
			int criticalPartialCap
	) {
		if (criticalMissingCount >= 2) {
			return Math.min(baseScore, MULTIPLE_CRITICAL_MISSING_CAP);
		}
		if (criticalMissingCount == 1) {
			return Math.min(baseScore, SINGLE_CRITICAL_MISSING_CAP);
		}
		if (criticalPartialCount > 0) {
			return Math.min(baseScore, criticalPartialCap);
		}
		return baseScore;
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

	private enum ScoreBand {
		HIGH,
		MEDIUM,
		LOW;

		private static ScoreBand fromExpectedRange(int minExpectedScore, int maxExpectedScore) {
			if (minExpectedScore >= 85) {
				return HIGH;
			}
			if (maxExpectedScore <= 69) {
				return LOW;
			}
			return MEDIUM;
		}

		private static ScoreBand fromScore(int score) {
			if (score >= 80) {
				return HIGH;
			}
			if (score >= 70) {
				return MEDIUM;
			}
			return LOW;
		}
	}
}
