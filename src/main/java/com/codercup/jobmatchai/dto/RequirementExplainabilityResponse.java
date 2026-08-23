package com.codercup.jobmatchai.dto;

public record RequirementExplainabilityResponse(
		String evidenceBasis,
		boolean statusAdjusted,
		String originalStatus,
		Boolean cvCatalogEvidenceDetected,
		Boolean jobCatalogEvidenceDetected,
		String summary
) {
}
