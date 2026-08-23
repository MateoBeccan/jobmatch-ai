package com.codercup.jobmatchai.evaluation;

import com.codercup.jobmatchai.scoring.RequirementAssessment;
import com.codercup.jobmatchai.scoring.RequirementCategory;
import com.codercup.jobmatchai.scoring.RequirementCriticality;
import com.codercup.jobmatchai.scoring.RequirementStatus;

record GoldenRequirement(
		String name,
		RequirementCategory category,
		RequirementCriticality criticality,
		RequirementStatus expectedStatus
) {

	RequirementAssessment toAssessment() {
		return new RequirementAssessment(
				name,
				category,
				criticality,
				expectedStatus,
				"Golden expected evidence."
		);
	}
}
