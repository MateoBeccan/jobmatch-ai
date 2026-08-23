package com.codercup.jobmatchai.evaluation;

import com.codercup.jobmatchai.scoring.MatchScoreResult;
import com.codercup.jobmatchai.scoring.ScoreBreakdown;
import com.codercup.jobmatchai.service.ProfessionalDomain;
import java.util.Set;

record ScoringCalibrationSnapshot(
		String id,
		ProfessionalDomain domain,
		Set<String> tags,
		int minExpectedScore,
		int maxExpectedScore,
		int baseScore,
		int finalScore,
		Integer mandatoryTechnical,
		Integer experienceSeniority,
		Integer desirable,
		Integer complementary,
		int criticalMissingCount,
		int criticalPartialCount,
		boolean criticalCapApplied
) {

	static ScoringCalibrationSnapshot from(GoldenAnalysisCase analysisCase, MatchScoreResult result) {
		ScoreBreakdown breakdown = result.breakdown();
		return new ScoringCalibrationSnapshot(
				analysisCase.id(),
				analysisCase.domain(),
				analysisCase.tags(),
				analysisCase.minExpectedScore(),
				analysisCase.maxExpectedScore(),
				result.basePercentage(),
				result.matchPercentage(),
				breakdown.mandatoryTechnical(),
				breakdown.experienceSeniority(),
				breakdown.desirable(),
				breakdown.complementary(),
				result.criticalMissingCount(),
				result.criticalPartialCount(),
				result.criticalCapApplied()
		);
	}
}
