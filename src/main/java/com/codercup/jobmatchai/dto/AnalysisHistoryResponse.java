package com.codercup.jobmatchai.dto;

import java.time.Instant;

public record AnalysisHistoryResponse(
		String id,
		String role,
		String company,
		String cvFileName,
		String cvVersion,
		String jobDescription,
		String mode,
		Integer score,
		Instant createdAt,
		AnalysisResponse result
) {
}