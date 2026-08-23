package com.codercup.jobmatchai.scoring;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MatchScoreCalculator {

	private static final int CRITICAL_PARTIAL_SCORE_CAP = 79;
	private static final int SINGLE_CRITICAL_MISSING_SCORE_CAP = 69;
	private static final int MULTIPLE_CRITICAL_MISSING_SCORE_CAP = 54;

	/**
	 * Calculates the category-weighted base score, then caps the final score when
	 * critical requirements from the job offer are not fully met. The breakdown
	 * continues to describe the uncapped category scores.
	 */
	public MatchScoreResult calculate(List<RequirementAssessment> requirements) {
		if (requirements == null || requirements.isEmpty()) {
			return new MatchScoreResult(0, emptyBreakdown(), 0, false, 0, 0);
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

		int basePercentage = totalPresentWeight == 0
				? 0
				: (int) Math.round((weightedPoints / totalPresentWeight) * 100);
		CriticalRequirementPolicyResult criticalPolicy = applyCriticalRequirementPolicy(basePercentage, requirements);

		return new MatchScoreResult(
				criticalPolicy.finalScore(),
				buildBreakdown(accumulators),
				basePercentage,
				criticalPolicy.capApplied(),
				criticalPolicy.criticalMissingCount(),
				criticalPolicy.criticalPartialCount()
		);
	}

	private CriticalRequirementPolicyResult applyCriticalRequirementPolicy(
			int baseScore,
			List<RequirementAssessment> requirements
	) {
		int criticalMissing = 0;
		int criticalPartial = 0;
		for (RequirementAssessment requirement : requirements) {
			if (requirement.criticality() != RequirementCriticality.CRITICAL) {
				continue;
			}
			if (requirement.status() == RequirementStatus.MISSING) {
				criticalMissing++;
			}
			if (requirement.status() == RequirementStatus.PARTIAL) {
				criticalPartial++;
			}
		}

		int finalScore = baseScore;
		if (criticalMissing >= 2) {
			finalScore = Math.min(baseScore, MULTIPLE_CRITICAL_MISSING_SCORE_CAP);
		}
		else if (criticalMissing == 1) {
			finalScore = Math.min(baseScore, SINGLE_CRITICAL_MISSING_SCORE_CAP);
		}
		else if (criticalPartial > 0) {
			finalScore = Math.min(baseScore, CRITICAL_PARTIAL_SCORE_CAP);
		}
		return new CriticalRequirementPolicyResult(
				finalScore,
				finalScore < baseScore,
				criticalMissing,
				criticalPartial
		);
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

	private record CriticalRequirementPolicyResult(
			int finalScore,
			boolean capApplied,
			int criticalMissingCount,
			int criticalPartialCount
	) {
	}
}
