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
		JobSearchProfileResponse jobSearchProfile,
		ScoreExplanationResponse scoreExplanation
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

	public AnalysisResponse(
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
		this(
				matchPercentage,
				matchingSkills,
				missingSkills,
				criticalMissingRequirements,
				experienceGap,
				warnings,
				recommendations,
				interviewQuestions,
				requirements,
				breakdown,
				jobSearchProfile,
				null
		);
	}

	private static <T> List<T> listOrEmpty(List<T> values) {
		return values == null ? List.of() : List.copyOf(values);
	}
}
