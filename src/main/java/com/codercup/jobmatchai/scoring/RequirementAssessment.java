package com.codercup.jobmatchai.scoring;

import java.util.Objects;

public record RequirementAssessment(
		String name,
		RequirementCategory category,
		RequirementCriticality criticality,
		RequirementStatus status,
		String evidence
) {

	public RequirementAssessment(String name, RequirementCategory category, RequirementStatus status, String evidence) {
		this(name, category, RequirementCriticality.NORMAL, status, evidence);
	}

	public RequirementAssessment {
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(category, "category must not be null");
		Objects.requireNonNull(criticality, "criticality must not be null");
		Objects.requireNonNull(status, "status must not be null");
		if (name.isBlank()) {
			throw new IllegalArgumentException("name must not be blank");
		}
	}
}
