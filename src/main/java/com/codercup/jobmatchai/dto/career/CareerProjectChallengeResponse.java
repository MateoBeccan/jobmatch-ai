package com.codercup.jobmatchai.dto.career;

import java.util.List;

public record CareerProjectChallengeResponse(
		String title,
		String description,
		List<String> skills
) {
}
