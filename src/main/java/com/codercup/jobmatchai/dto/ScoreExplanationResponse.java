package com.codercup.jobmatchai.dto;

public record ScoreExplanationResponse(
		Integer basePercentage,
		Integer finalPercentage,
		boolean criticalCapApplied,
		int criticalMissingCount,
		int criticalPartialCount,
		String capReason,
		String summary
) {
}
