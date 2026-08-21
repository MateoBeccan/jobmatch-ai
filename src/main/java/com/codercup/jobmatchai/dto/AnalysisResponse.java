package com.codercup.jobmatchai.dto;

import java.util.List;

public record AnalysisResponse(
		Integer matchPercentage,
		List<String> matchingSkills,
		List<String> missingSkills,
		List<CriticalRequirementGapResponse> criticalMissingRequirements,
		ExperienceGapResponse experienceGap,
		List<String> warnings,
		List<String> recommendations,
		List<String> interviewQuestions,
		List<RequirementResponse> requirements,
		ScoreBreakdownResponse breakdown,
		JobSearchProfileResponse jobSearchProfile
) {

	public AnalysisResponse {
		matchingSkills = listOrEmpty(matchingSkills);
		missingSkills = listOrEmpty(missingSkills);
		criticalMissingRequirements = listOrEmpty(criticalMissingRequirements);
		warnings = listOrEmpty(warnings);
		recommendations = listOrEmpty(recommendations);
		interviewQuestions = listOrEmpty(interviewQuestions);
		requirements = listOrEmpty(requirements);
	}

	private static <T> List<T> listOrEmpty(List<T> values) {
		return values == null ? List.of() : List.copyOf(values);
	}
}
