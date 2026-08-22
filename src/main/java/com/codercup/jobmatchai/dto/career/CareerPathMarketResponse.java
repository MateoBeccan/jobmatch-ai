package com.codercup.jobmatchai.dto.career;

import java.util.List;

public record CareerPathMarketResponse(
		int sampleSize,
		CareerMarketConfidence confidence,
		int coveragePercentage,
		List<String> currentSkillsDetected,
		List<CareerSkillDemandResponse> missingSkills,
		List<CareerSkillDemandResponse> skillDemand
) {
}
