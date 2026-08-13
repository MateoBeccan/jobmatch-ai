package com.codercup.jobmatchai.dto;

import java.util.List;

public record AnalysisHistoryPageResponse(
		List<AnalysisSummaryResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages
) {
}
