package com.codercup.jobmatchai.dto;

public record RequirementResponse(
		String name,
		String category,
		String criticality,
		String status,
		String evidence,
		RequirementExplainabilityResponse explainability
) {

	public RequirementResponse(
			String name,
			String category,
			String status,
			String evidence
	) {
		this(name, category, null, status, evidence, null);
	}
}
