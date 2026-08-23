package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.dto.internal.GeminiAnalysisResult;
import com.codercup.jobmatchai.dto.internal.GeminiJobSearchProfile;
import com.codercup.jobmatchai.scoring.RequirementAssessment;
import com.codercup.jobmatchai.scoring.RequirementCategory;
import com.codercup.jobmatchai.scoring.RequirementStatus;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AnalysisEvidenceValidator {

	public GeminiAnalysisResult validate(
			GeminiAnalysisResult result,
			List<ProfessionalKnowledgeEntry> cvKnowledge,
			List<ProfessionalKnowledgeEntry> jobKnowledge,
			boolean hasTextualJobEvidence
	) {
		Set<String> cvKeys = canonicalKeys(cvKnowledge);
		Set<String> jobKeys = canonicalKeys(jobKnowledge);
		List<String> matchingSkills = validateMatchingSkills(result.matchingSkills(), cvKeys);
		List<String> missingSkills = validateMissingSkills(
				result.missingSkills(),
				matchingSkills,
				cvKeys,
				jobKeys,
				hasTextualJobEvidence
		);
		List<RequirementAssessment> requirements = validateRequirements(result.requirements(), cvKeys);
		GeminiJobSearchProfile profile = validateJobSearchProfile(result.jobSearchProfile(), cvKnowledge, cvKeys);

		return new GeminiAnalysisResult(
				requirements,
				matchingSkills,
				missingSkills,
				result.recommendations(),
				result.interviewQuestions(),
				profile
		);
	}

	private List<String> validateMatchingSkills(List<String> values, Set<String> cvKeys) {
		List<String> normalized = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		for (String value : values) {
			Optional<ProfessionalKnowledgeEntry> entry = ProfessionalKnowledgeCatalog.findByCanonicalOrAlias(value);
			if (entry.isPresent() && !cvKeys.contains(canonicalKey(entry.get()))) {
				continue;
			}
			String output = entry.map(ProfessionalKnowledgeEntry::canonicalName).orElse(value);
			if (seen.add(ProfessionalKnowledgeCatalog.comparisonKey(output))) {
				normalized.add(output);
			}
		}
		return List.copyOf(normalized);
	}

	private List<String> validateMissingSkills(
			List<String> values,
			List<String> matchingSkills,
			Set<String> cvKeys,
			Set<String> jobKeys,
			boolean hasTextualJobEvidence
	) {
		Set<String> matchingKeys = keysFromValues(matchingSkills);
		List<String> normalized = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		for (String value : values) {
			Optional<ProfessionalKnowledgeEntry> entry = ProfessionalKnowledgeCatalog.findByCanonicalOrAlias(value);
			if (entry.isPresent()) {
				String key = canonicalKey(entry.get());
				if (cvKeys.contains(key)) {
					continue;
				}
				if (hasTextualJobEvidence && !jobKeys.contains(key)) {
					continue;
				}
				if (matchingKeys.contains(key)) {
					continue;
				}
				if (seen.add(key)) {
					normalized.add(entry.get().canonicalName());
				}
				continue;
			}
			String key = ProfessionalKnowledgeCatalog.comparisonKey(value);
			if (matchingKeys.contains(key)) {
				continue;
			}
			if (seen.add(key)) {
				normalized.add(value);
			}
		}
		return List.copyOf(normalized);
	}

	private List<RequirementAssessment> validateRequirements(
			List<RequirementAssessment> requirements,
			Set<String> cvKeys
	) {
		return requirements.stream()
				.map(requirement -> validateRequirement(requirement, cvKeys))
				.toList();
	}

	private RequirementAssessment validateRequirement(RequirementAssessment requirement, Set<String> cvKeys) {
		if (requirement.category() == RequirementCategory.EXPERIENCE_SENIORITY) {
			return requirement;
		}
		Optional<ProfessionalKnowledgeEntry> entry = ProfessionalKnowledgeCatalog.findByCanonicalOrAlias(requirement.name());
		if (entry.isEmpty()) {
			return requirement;
		}
		boolean cvContainsKnowledge = cvKeys.contains(canonicalKey(entry.get()));
		RequirementStatus status = requirement.status();
		if (cvContainsKnowledge && status == RequirementStatus.MISSING) {
			status = RequirementStatus.PARTIAL;
		}
		else if (!cvContainsKnowledge && status != RequirementStatus.MISSING) {
			status = RequirementStatus.MISSING;
		}
		if (status == requirement.status()) {
			return requirement;
		}
		return new RequirementAssessment(
				requirement.name(),
				requirement.category(),
				requirement.criticality(),
				status,
				requirement.evidence()
		);
	}

	private GeminiJobSearchProfile validateJobSearchProfile(
			GeminiJobSearchProfile profile,
			List<ProfessionalKnowledgeEntry> cvKnowledge,
			Set<String> cvKeys
	) {
		List<String> keywords = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		for (String keyword : profile.keywords()) {
			Optional<ProfessionalKnowledgeEntry> entry = ProfessionalKnowledgeCatalog.findByCanonicalOrAlias(keyword);
			if (entry.isPresent() && !cvKeys.contains(canonicalKey(entry.get()))) {
				continue;
			}
			String output = entry.map(ProfessionalKnowledgeEntry::canonicalName).orElse(keyword);
			if (seen.add(ProfessionalKnowledgeCatalog.comparisonKey(output))) {
				keywords.add(output);
			}
		}
		for (ProfessionalKnowledgeEntry entry : cvKnowledge) {
			if (keywords.size() >= 3) {
				break;
			}
			if (seen.add(canonicalKey(entry))) {
				keywords.add(entry.canonicalName());
			}
		}
		return new GeminiJobSearchProfile(profile.role(), profile.seniority(), List.copyOf(keywords));
	}

	private Set<String> canonicalKeys(List<ProfessionalKnowledgeEntry> entries) {
		return entries.stream()
				.map(this::canonicalKey)
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
	}

	private Set<String> keysFromValues(List<String> values) {
		Set<String> keys = new LinkedHashSet<>();
		for (String value : values) {
			Optional<ProfessionalKnowledgeEntry> entry = ProfessionalKnowledgeCatalog.findByCanonicalOrAlias(value);
			keys.add(entry.map(this::canonicalKey).orElseGet(() -> ProfessionalKnowledgeCatalog.comparisonKey(value)));
		}
		return keys;
	}

	private String canonicalKey(ProfessionalKnowledgeEntry entry) {
		return ProfessionalKnowledgeCatalog.comparisonKey(entry.canonicalName());
	}
}
