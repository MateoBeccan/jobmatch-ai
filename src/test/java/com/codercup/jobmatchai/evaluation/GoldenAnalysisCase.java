package com.codercup.jobmatchai.evaluation;

import com.codercup.jobmatchai.dto.internal.GeminiAnalysisResult;
import com.codercup.jobmatchai.service.ProfessionalDomain;
import java.util.List;
import java.util.Set;

record GoldenAnalysisCase(
		String id,
		ProfessionalDomain domain,
		String name,
		String purpose,
		String cvText,
		String jobDescription,
		List<GoldenRequirement> expectedRequirements,
		List<String> expectedMatchingSkills,
		List<String> expectedMissingSkills,
		List<String> expectedCriticalRequirements,
		String expectedExperienceGap,
		int minExpectedScore,
		int maxExpectedScore,
		Set<String> tags,
		GeminiAnalysisResult modelResult
) {
}
