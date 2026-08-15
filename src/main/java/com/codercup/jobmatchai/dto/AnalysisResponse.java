package com.codercup.jobmatchai.dto;

import java.util.List;

public record AnalysisResponse(
		Integer matchPercentage,
		List<String> matchingSkills,
		List<String> missingSkills,
		List<String> recommendations,
		List<String> interviewQuestions,
		List<RequirementResponse> requirements,
		ScoreBreakdownResponse breakdown
) {
}
