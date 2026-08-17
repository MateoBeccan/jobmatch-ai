package com.codercup.jobmatchai.dto.internal;

import com.codercup.jobmatchai.scoring.RequirementAssessment;
import java.util.List;

public record GeminiAnalysisResult(
		List<RequirementAssessment> requirements,
		List<String> matchingSkills,
		List<String> missingSkills,
		List<String> recommendations,
		List<String> interviewQuestions,
		GeminiJobSearchProfile jobSearchProfile
) {
}
