package com.codercup.jobmatchai.dto.career;

public record CareerLearningPriorityResponse(
		String skill,
		int jobsMentioning,
		int frequencyPercentage,
		CareerLearningPriority priority
) {
}
