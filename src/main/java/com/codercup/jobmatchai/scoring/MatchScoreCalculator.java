package com.codercup.jobmatchai.scoring;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MatchScoreCalculator {

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

		return new MatchScoreResult(matchPercentage, buildBreakdown(accumulators));
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
