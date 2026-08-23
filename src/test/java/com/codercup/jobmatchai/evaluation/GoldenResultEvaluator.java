package com.codercup.jobmatchai.evaluation;

import com.codercup.jobmatchai.dto.internal.GeminiAnalysisResult;
import com.codercup.jobmatchai.scoring.MatchScoreCalculator;
import com.codercup.jobmatchai.scoring.RequirementAssessment;
import com.codercup.jobmatchai.scoring.RequirementCriticality;
import com.codercup.jobmatchai.scoring.RequirementStatus;
import com.codercup.jobmatchai.service.ProfessionalKnowledgeCatalog;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class GoldenResultEvaluator {

	private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}");
	private static final Pattern COMPARISON_SEPARATOR_PATTERN = Pattern.compile("[^a-z0-9#+]+");

	private final MatchScoreCalculator matchScoreCalculator = new MatchScoreCalculator();

	GoldenEvaluationResult evaluate(GoldenAnalysisCase expected, GeminiAnalysisResult actual) {
		RequirementComparison requirementComparison = compareRequirements(expected.expectedRequirements(), actual.requirements());
		SkillPrecision matchingSkillPrecision = precision(expected.expectedMatchingSkills(), actual.matchingSkills());
		SkillPrecision missingSkillPrecision = precision(expected.expectedMissingSkills(), actual.missingSkills());
		Set<String> expectedCriticalGaps = normalizedRequirementSet(expected.expectedCriticalRequirements());
		Set<String> actualCriticalGaps = actual.requirements().stream()
				.filter(requirement -> requirement.criticality() == RequirementCriticality.CRITICAL)
				.filter(requirement -> requirement.status() == RequirementStatus.MISSING)
				.map(requirement -> requirementKey(requirement.name()))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		int actualScore = matchScoreCalculator.calculate(actual.requirements()).matchPercentage();

		return new GoldenEvaluationResult(
				expected.id(),
				requirementComparison.accuracy(),
				matchingSkillPrecision.value(),
				missingSkillPrecision.value(),
				exactSetAccuracy(expectedCriticalGaps, actualCriticalGaps),
				matchingSkillPrecision.hallucinatedKnownSkillsCount()
						+ missingSkillPrecision.hallucinatedKnownSkillsCount(),
				actualScore >= expected.minExpectedScore() && actualScore <= expected.maxExpectedScore(),
				actualScore,
				requirementComparison.expectedNotFound(),
				requirementComparison.unexpected()
		);
	}

	GoldenEvaluationSummary summarize(List<GoldenAnalysisCase> cases, List<GoldenEvaluationResult> results) {
		return new GoldenEvaluationSummary(
				cases.size(),
				cases.stream().collect(Collectors.groupingBy(
						GoldenAnalysisCase::domain,
						LinkedHashMap::new,
						Collectors.counting()
				)),
				cases.stream().collect(Collectors.groupingBy(
						this::expectedBand,
						LinkedHashMap::new,
						Collectors.counting()
				)),
				average(results, result -> result.requirementStatusAccuracy().orElse(Double.NaN)),
				average(results, result -> result.matchingSkillPrecision().orElse(Double.NaN)),
				average(results, result -> result.missingSkillPrecision().orElse(Double.NaN)),
				average(results, result -> result.criticalGapAccuracy().orElse(Double.NaN)),
				results.stream().mapToInt(GoldenEvaluationResult::hallucinatedKnownSkillsCount).sum(),
				results.isEmpty()
						? OptionalDouble.empty()
						: OptionalDouble.of(results.stream()
								.filter(GoldenEvaluationResult::scoreWithinExpectedRange)
								.count() / (double) results.size())
		);
	}

	private RequirementComparison compareRequirements(
			List<GoldenRequirement> expected,
			List<RequirementAssessment> actual
	) {
		Map<String, GoldenRequirement> expectedByKey = expected.stream()
				.collect(Collectors.toMap(
						requirement -> requirementKey(requirement.name()),
						requirement -> requirement,
						(first, second) -> first,
						LinkedHashMap::new
				));
		Map<String, RequirementAssessment> actualByKey = actual.stream()
				.collect(Collectors.toMap(
						requirement -> requirementKey(requirement.name()),
						requirement -> requirement,
						(first, second) -> first,
						LinkedHashMap::new
				));
		int comparable = 0;
		int correct = 0;
		List<String> expectedNotFound = new ArrayList<>();
		for (GoldenRequirement expectedRequirement : expected) {
			RequirementAssessment actualRequirement = actualByKey.get(requirementKey(expectedRequirement.name()));
			if (actualRequirement == null) {
				expectedNotFound.add(expectedRequirement.name());
				continue;
			}
			comparable++;
			if (actualRequirement.status() == expectedRequirement.expectedStatus()) {
				correct++;
			}
		}
		List<String> unexpected = actual.stream()
				.filter(requirement -> !expectedByKey.containsKey(requirementKey(requirement.name())))
				.map(RequirementAssessment::name)
				.toList();
		return new RequirementComparison(
				comparable == 0 ? OptionalDouble.empty() : OptionalDouble.of(correct / (double) comparable),
				expectedNotFound,
				unexpected
		);
	}

	private SkillPrecision precision(List<String> expectedValues, List<String> actualValues) {
		Set<String> expected = normalizedSkillSet(expectedValues);
		int truePositive = 0;
		int hallucinatedKnown = 0;
		Set<String> seenActual = new LinkedHashSet<>();
		for (String actual : actualValues) {
			String key = skillKey(actual);
			if (!seenActual.add(key)) {
				continue;
			}
			if (expected.contains(key)) {
				truePositive++;
			}
			else if (ProfessionalKnowledgeCatalog.findByCanonicalOrAlias(actual).isPresent()) {
				hallucinatedKnown++;
			}
		}
		return new SkillPrecision(
				seenActual.isEmpty() ? OptionalDouble.empty() : OptionalDouble.of(truePositive / (double) seenActual.size()),
				hallucinatedKnown
		);
	}

	private OptionalDouble exactSetAccuracy(Set<String> expected, Set<String> actual) {
		if (expected.isEmpty() && actual.isEmpty()) {
			return OptionalDouble.empty();
		}
		return OptionalDouble.of(expected.equals(actual) ? 1.0 : 0.0);
	}

	private OptionalDouble average(List<GoldenEvaluationResult> results, ToDoubleFunction<GoldenEvaluationResult> value) {
		double[] values = results.stream()
				.mapToDouble(value)
				.filter(item -> !Double.isNaN(item))
				.toArray();
		if (values.length == 0) {
			return OptionalDouble.empty();
		}
		return OptionalDouble.of(java.util.Arrays.stream(values).average().orElseThrow());
	}

	private Set<String> normalizedRequirementSet(List<String> values) {
		return values.stream()
				.map(this::requirementKey)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private Set<String> normalizedSkillSet(List<String> values) {
		return values.stream()
				.map(this::skillKey)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private String requirementKey(String value) {
		return ProfessionalKnowledgeCatalog.findByCanonicalOrAlias(value)
				.map(entry -> normalize(entry.canonicalName()))
				.orElseGet(() -> normalize(value));
	}

	private String skillKey(String value) {
		return ProfessionalKnowledgeCatalog.findByCanonicalOrAlias(value)
				.map(entry -> normalize(entry.canonicalName()))
				.orElseGet(() -> normalize(value));
	}

	private String normalize(String raw) {
		String withoutAccents = Normalizer.normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
		String normalized = DIACRITICS_PATTERN.matcher(withoutAccents).replaceAll("");
		return COMPARISON_SEPARATOR_PATTERN.matcher(normalized).replaceAll("");
	}

	private String expectedBand(GoldenAnalysisCase analysisCase) {
		if (analysisCase.minExpectedScore() >= 80) {
			return "HIGH";
		}
		if (analysisCase.maxExpectedScore() < 70) {
			return "LOW";
		}
		return "MEDIUM";
	}

	private record RequirementComparison(
			OptionalDouble accuracy,
			List<String> expectedNotFound,
			List<String> unexpected
	) {
	}

	private record SkillPrecision(
			OptionalDouble value,
			int hallucinatedKnownSkillsCount
	) {
	}
}
