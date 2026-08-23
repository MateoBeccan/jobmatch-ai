package com.codercup.jobmatchai.evaluation;

import com.codercup.jobmatchai.service.ProfessionalDomain;
import java.util.Map;
import java.util.OptionalDouble;

record GoldenEvaluationSummary(
		int totalCases,
		Map<ProfessionalDomain, Long> casesByDomain,
		Map<String, Long> casesByExpectedBand,
		OptionalDouble requirementStatusAccuracy,
		OptionalDouble matchingSkillPrecision,
		OptionalDouble missingSkillPrecision,
		OptionalDouble criticalGapAccuracy,
		int hallucinatedKnownSkillsCount,
		OptionalDouble scoreRangePassRate
) {
}
