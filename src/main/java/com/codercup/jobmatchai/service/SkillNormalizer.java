package com.codercup.jobmatchai.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SkillNormalizer {

	private SkillNormalizer() {
	}

	public static String canonicalizeSkill(String raw) {
		String trimmed = raw.trim();
		return technicalEntryByAlias(trimmed)
				.map(ProfessionalKnowledgeEntry::canonicalName)
				.orElse(trimmed);
	}

	public static String comparisonKey(String raw) {
		return ProfessionalKnowledgeCatalog.comparisonKey(raw);
	}

	public static List<String> normalizeSkillList(List<String> skills) {
		List<String> normalized = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		for (String skill : skills) {
			String canonical = canonicalizeSkill(skill);
			if (seen.add(comparisonKey(canonical))) {
				normalized.add(canonical);
			}
		}
		return List.copyOf(normalized);
	}

	public static boolean equivalentSkill(String first, String second) {
		return comparisonKey(canonicalizeSkill(first)).equals(comparisonKey(canonicalizeSkill(second)));
	}

	public static boolean isCanonicalSkill(String raw) {
		return technicalEntryByAlias(raw).isPresent();
	}

	public static List<String> canonicalSkills() {
		return ProfessionalKnowledgeCatalog.technicalMarketCanonicalNames();
	}

	public static boolean containsCanonicalSkill(String text, String canonicalSkill) {
		Optional<ProfessionalKnowledgeEntry> entry = technicalEntryByAlias(canonicalSkill);
		if (entry.isEmpty() || !equivalentSkill(entry.get().canonicalName(), canonicalSkill)) {
			return false;
		}
		return entry.get().aliases().stream()
				.anyMatch(alias -> ProfessionalKnowledgeCatalog.matchesAlias(text, entry.get(), alias));
	}

	private static Optional<ProfessionalKnowledgeEntry> technicalEntryByAlias(String value) {
		Optional<ProfessionalKnowledgeEntry> entry = ProfessionalKnowledgeCatalog.findByAlias(value);
		return entry.filter(SkillNormalizer::isTechnicalMarketEntry);
	}

	private static boolean isTechnicalMarketEntry(ProfessionalKnowledgeEntry entry) {
		return ProfessionalKnowledgeCatalog.technicalMarketEntries().stream()
				.anyMatch(technical -> technical.canonicalName().equals(entry.canonicalName()));
	}
}
