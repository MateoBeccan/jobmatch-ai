package com.codercup.jobmatchai.dto;

public record ExperienceGapResponse(
		String requirement,
		String status,
		Boolean critical,
		String summary
) {
}
