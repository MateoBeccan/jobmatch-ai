package com.codercup.jobmatchai.dto.career.internal;

import com.codercup.jobmatchai.dto.JobSeniority;
import java.util.List;

public record CareerProfile(
		String role,
		JobSeniority seniority,
		List<String> skills
) {
}
