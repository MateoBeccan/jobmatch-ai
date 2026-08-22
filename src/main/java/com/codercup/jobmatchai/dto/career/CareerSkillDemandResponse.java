package com.codercup.jobmatchai.dto.career;

public record CareerSkillDemandResponse(
		String skill,
		int jobsMentioning,
		int frequencyPercentage
) {
}
