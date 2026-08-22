package com.codercup.jobmatchai.dto.career.internal;

import com.codercup.jobmatchai.dto.career.CareerPathType;
import java.util.List;

public record CareerGeminiPath(
		CareerPathType type,
		String role,
		List<String> aliases,
		String summary,
		String rationale,
		List<String> candidateSkills
) {
}
