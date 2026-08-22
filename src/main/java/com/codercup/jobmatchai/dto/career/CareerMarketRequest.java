package com.codercup.jobmatchai.dto.career;

import com.codercup.jobmatchai.dto.JobSeniority;
import java.util.List;

public record CareerMarketRequest(
		String role,
		JobSeniority seniority,
		List<String> currentSkills,
		CareerRegion region,
		List<String> roleAliases
) {
	public CareerMarketRequest(String role, JobSeniority seniority, List<String> currentSkills, CareerRegion region) {
		this(role, seniority, currentSkills, region, List.of());
	}
}
