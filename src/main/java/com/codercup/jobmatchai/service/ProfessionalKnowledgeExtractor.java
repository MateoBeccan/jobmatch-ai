package com.codercup.jobmatchai.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ProfessionalKnowledgeExtractor {

	public List<ProfessionalKnowledgeEntry> extract(String text) {
		if (text == null || text.isBlank()) {
			return List.of();
		}
		List<ProfessionalKnowledgeEntry> detected = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		for (ProfessionalKnowledgeEntry entry : ProfessionalKnowledgeCatalog.allEntries()) {
			boolean matches = entry.aliases().stream()
					.anyMatch(alias -> ProfessionalKnowledgeCatalog.matchesAlias(text, entry, alias));
			if (matches && seen.add(ProfessionalKnowledgeCatalog.comparisonKey(entry.canonicalName()))) {
				detected.add(entry);
			}
		}
		return List.copyOf(detected);
	}

	public List<String> extractCanonicalNames(String text) {
		return extract(text).stream()
				.map(ProfessionalKnowledgeEntry::canonicalName)
				.toList();
	}
}
