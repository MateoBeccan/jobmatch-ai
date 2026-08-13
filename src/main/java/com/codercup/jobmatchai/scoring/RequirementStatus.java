package com.codercup.jobmatchai.scoring;

public enum RequirementStatus {
	MATCH(1.0),
	PARTIAL(0.5),
	MISSING(0.0);

	private final double factor;

	RequirementStatus(double factor) {
		this.factor = factor;
	}

	public double factor() {
		return factor;
	}
}
