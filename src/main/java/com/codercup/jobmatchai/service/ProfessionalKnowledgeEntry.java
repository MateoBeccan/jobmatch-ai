package com.codercup.jobmatchai.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record ProfessionalKnowledgeEntry(
		String canonicalName,
		List<String> aliases,
		Set<ProfessionalDomain> domains,
		KnowledgeType type
) {

	public ProfessionalKnowledgeEntry {
		if (canonicalName == null || canonicalName.isBlank()) {
			throw new IllegalArgumentException("canonicalName is required");
		}
		if (aliases == null || aliases.isEmpty()) {
			throw new IllegalArgumentException("aliases are required");
		}
		if (domains == null || domains.isEmpty()) {
			throw new IllegalArgumentException("domains are required");
		}
		if (type == null) {
			throw new IllegalArgumentException("type is required");
		}
		canonicalName = canonicalName.trim();
		aliases = aliases.stream()
				.filter(alias -> alias != null && !alias.isBlank())
				.map(String::trim)
				.collect(java.util.stream.Collectors.collectingAndThen(
						java.util.stream.Collectors.toCollection(LinkedHashSet::new),
						List::copyOf
				));
		if (aliases.isEmpty()) {
			throw new IllegalArgumentException("aliases are required");
		}
		domains = Set.copyOf(new LinkedHashSet<>(domains));
	}
}
