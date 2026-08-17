package com.codercup.jobmatchai.dto;

import java.util.List;

public record JobOfferResponse(
		String id,
		String title,
		String company,
		String location,
		String snippet,
		String salary,
		String employmentType,
		String updatedAt,
		String url,
		String source,
		List<String> matchedKeywords
) {
}
