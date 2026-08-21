package com.codercup.jobmatchai.scoring;

public record MatchScoreResult(
		Integer matchPercentage,
		ScoreBreakdown breakdown,
		Integer basePercentage,
		boolean criticalCapApplied,
		int criticalMissingCount,
		int criticalPartialCount
) {
}
