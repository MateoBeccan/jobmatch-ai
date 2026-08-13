package com.codercup.jobmatchai.dto;

import java.time.Instant;

public record AnalysisSummaryResponse(
		String id,
		String role,
		String company,
		String cvFileName,
		String cvVersion,
		String mode,
		Integer score,
		Instant createdAt
) {
}
