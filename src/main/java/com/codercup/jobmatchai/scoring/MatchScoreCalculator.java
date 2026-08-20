package com.codercup.jobmatchai.scoring;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MatchScoreCalculator {

	private static final int CRITICAL_PARTIAL_SCORE_CAP = 85;
	private static final int SINGLE_CRITICAL_MISSING_SCORE_CAP = 69;
	private static final int MULTIPLE_CRITICAL_MISSING_SCORE_CAP = 54;

	/**
	 * Calculates the category-weighted base score, then caps the final score when
	 * critical requirements from the job offer are not fully met. The breakdown
	 * continues to describe the uncapped category scores.
	 */
	public MatchScoreResult calculate(List<RequirementAssessment> requirements) {
		if (requirements == null || requirements.isEmpty()) {
			return new MatchScoreResult(0, emptyBreakdown());
		}

		Map<RequirementCategory, CategoryAccumulator> accumulators = new EnumMap<>(RequirementCategory.class);
		for (RequirementAssessment requirement : requirements) {
			accumulators.computeIfAbsent(requirement.category(), ignored -> new CategoryAccumulator())
					.add(requirement.status().factor());
		}

		double weightedPoints = 0.0;
		int totalPresentWeight = 0;
		for (Map.Entry<RequirementCategory, CategoryAccumulator> entry : accumulators.entrySet()) {
			RequirementCategory category = entry.getKey();
			double ratio = entry.getValue().ratio();
			weightedPoints += ratio * category.weight();
			totalPresentWeight += category.weight();
		}

		int matchPercentage = totalPresentWeight == 0
				? 0
				: (int) Math.round((weightedPoints / totalPresentWeight) * 100);

		return new MatchScoreResult(applyCriticalRequirementPolicy(matchPercentage, requirements), buildBreakdown(accumulators));
	}

	private int applyCriticalRequirementPolicy(int baseScore, List<RequirementAssessment> requirements) {
		int criticalMissing = 0;
		boolean hasCriticalPartial = false;
		for (RequirementAssessment requirement : requirements) {
			if (requirement.criticality() != RequirementCriticality.CRITICAL) {
				continue;
			}
			if (requirement.status() == RequirementStatus.MISSING) {
				criticalMissing++;
			}
			if (requirement.status() == RequirementStatus.PARTIAL) {
				hasCriticalPartial = true;
			}
		}

		if (criticalMissing >= 2) {
			return Math.min(baseScore, MULTIPLE_CRITICAL_MISSING_SCORE_CAP);
		}
		if (criticalMissing == 1) {
			return Math.min(baseScore, SINGLE_CRITICAL_MISSING_SCORE_CAP);
		}
		if (hasCriticalPartial) {
			return Math.min(baseScore, CRITICAL_PARTIAL_SCORE_CAP);
		}
		return baseScore;
	}

	private ScoreBreakdown emptyBreakdown() {
		return new ScoreBreakdown(null, null, null, null);
	}

	private ScoreBreakdown buildBreakdown(Map<RequirementCategory, CategoryAccumulator> accumulators) {
		return new ScoreBreakdown(
				categoryScore(accumulators, RequirementCategory.MANDATORY_TECHNICAL),
				categoryScore(accumulators, RequirementCategory.EXPERIENCE_SENIORITY),
				categoryScore(accumulators, RequirementCategory.DESIRABLE),
				categoryScore(accumulators, RequirementCategory.COMPLEMENTARY)
		);
	}

	private Integer categoryScore(
			Map<RequirementCategory, CategoryAccumulator> accumulators,
			RequirementCategory category
	) {
		CategoryAccumulator accumulator = accumulators.get(category);
		if (accumulator == null) {
			return null;
		}
		return (int) Math.round(accumulator.ratio() * 100);
	}

	private static class CategoryAccumulator {

		private double factorSum;
		private int count;

		private void add(double factor) {
			factorSum += factor;
			count++;
		}

		private double ratio() {
			return factorSum / count;
		}
	}
}
