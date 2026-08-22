package com.codercup.jobmatchai.dto.career;

import java.util.List;

public record CareerPathResponse(
		CareerPathType type,
		String role,
		String summary,
		String rationale,
		CareerPathMarketResponse market,
		List<CareerLearningPriorityResponse> learningPriorities,
		List<CareerRoadmapStepResponse> roadmap,
		CareerProjectChallengeResponse projectChallenge
) {
}
