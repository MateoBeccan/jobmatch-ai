package com.codercup.jobmatchai.scoring;

public record MatchScoreResult(
		Integer matchPercentage,
		ScoreBreakdown breakdown
) {
}
