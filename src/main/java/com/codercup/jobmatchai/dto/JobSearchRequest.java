package com.codercup.jobmatchai.dto;

import java.util.List;

public record JobSearchRequest(
		String role,
		JobSeniority seniority,
		List<String> keywords,
		String location
) {
}
