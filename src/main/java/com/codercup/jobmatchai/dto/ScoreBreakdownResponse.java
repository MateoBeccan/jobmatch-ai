package com.codercup.jobmatchai.dto;

public record ScoreBreakdownResponse(
		Integer mandatoryTechnical,
		Integer experienceSeniority,
		Integer desirable,
		Integer complementary
) {
}
