package com.codercup.jobmatchai.scoring;

public enum RequirementCategory {
	MANDATORY_TECHNICAL(60),
	EXPERIENCE_SENIORITY(20),
	DESIRABLE(10),
	COMPLEMENTARY(10);

	private final int weight;

	RequirementCategory(int weight) {
		this.weight = weight;
	}

	public int weight() {
		return weight;
	}
}
