package com.codercup.jobmatchai.dto.career;

import java.util.List;

public record CareerMarketResponse(
		String provider,
		String role,
		CareerRegion region,
		int sampleSize,
		CareerMarketConfidence confidence,
		int coveragePercentage,
		List<String> currentSkillsDetected,
		List<CareerSkillDemandResponse> missingSkills,
		List<CareerSkillDemandResponse> skillDemand
) {
}
