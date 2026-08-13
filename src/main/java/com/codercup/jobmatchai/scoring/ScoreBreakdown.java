package com.codercup.jobmatchai.scoring;

public record ScoreBreakdown(
		Integer mandatoryTechnical,
		Integer experienceSeniority,
		Integer desirable,
		Integer complementary
) {
}
