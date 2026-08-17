package com.codercup.jobmatchai.dto;

import java.util.List;

public record JobSearchResponse(
		String provider,
		Integer count,
		List<JobOfferResponse> jobs
) {
}
