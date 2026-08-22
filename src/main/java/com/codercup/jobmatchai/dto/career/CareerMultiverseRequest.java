package com.codercup.jobmatchai.dto.career;

import com.codercup.jobmatchai.dto.JobSeniority;
import java.util.List;

public record CareerMultiverseRequest(
		String role,
		JobSeniority seniority,
		List<String> skills,
		CareerRegion region
) {
}
