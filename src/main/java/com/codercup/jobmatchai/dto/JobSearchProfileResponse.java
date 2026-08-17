package com.codercup.jobmatchai.dto;

import java.util.List;

public record JobSearchProfileResponse(
		String role,
		JobSeniority seniority,
		List<String> keywords
) {
}
