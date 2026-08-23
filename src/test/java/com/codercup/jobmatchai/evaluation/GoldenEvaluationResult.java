package com.codercup.jobmatchai.evaluation;

import java.util.List;
import java.util.OptionalDouble;

record GoldenEvaluationResult(
		String caseId,
		OptionalDouble requirementStatusAccuracy,
		OptionalDouble matchingSkillPrecision,
		OptionalDouble missingSkillPrecision,
		OptionalDouble criticalGapAccuracy,
		int hallucinatedKnownSkillsCount,
		boolean scoreWithinExpectedRange,
		int actualScore,
		List<String> expectedRequirementsNotFound,
		List<String> unexpectedRequirements
) {
}
