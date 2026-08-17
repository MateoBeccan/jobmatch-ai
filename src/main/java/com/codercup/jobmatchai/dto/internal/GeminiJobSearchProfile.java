package com.codercup.jobmatchai.dto.internal;

import com.codercup.jobmatchai.dto.JobSeniority;
import java.util.List;

public record GeminiJobSearchProfile(
		String role,
		JobSeniority seniority,
		List<String> keywords
) {
}
