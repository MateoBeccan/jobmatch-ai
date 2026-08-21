package com.codercup.jobmatchai.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class SkillNormalizer {

	private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}");
	private static final Pattern COMPARISON_SEPARATOR_PATTERN = Pattern.compile("[^a-z0-9#+]+");
	private static final Map<String, String> CANONICAL_SKILLS = buildCanonicalSkills();

	private SkillNormalizer() {
	}

	public static String canonicalizeSkill(String raw) {
		String trimmed = raw.trim();
		return CANONICAL_SKILLS.getOrDefault(comparisonKey(trimmed), trimmed);
	}

	public static String comparisonKey(String raw) {
		String withoutAccents = Normalizer.normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
		String normalized = DIACRITICS_PATTERN.matcher(withoutAccents).replaceAll("");
		return COMPARISON_SEPARATOR_PATTERN.matcher(normalized).replaceAll("");
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
		return CANONICAL_SKILLS.containsKey(comparisonKey(raw));
	}

	private static Map<String, String> buildCanonicalSkills() {
		Map<String, String> skills = new LinkedHashMap<>();
		register(skills, "Java", "java");
		register(skills, "JavaScript", "javascript", "js");
		register(skills, "TypeScript", "typescript", "ts");
		register(skills, "Node.js", "nodejs", "node.js", "node js");
		register(skills, "PostgreSQL", "postgresql", "postgres");
		register(skills, "Vue.js", "vue", "vuejs", "vue.js");
		register(skills, "React", "react", "reactjs", "react.js");
		register(skills, "Spring Boot", "spring boot", "springboot", "spring-boot");
		register(skills, "REST APIs", "rest api", "rest apis", "restful api", "restful apis",
				"restful service", "restful services");
		register(skills, "Docker", "docker");
		register(skills, "Kubernetes", "kubernetes", "k8s");
		register(skills, "C#", "c#", "csharp");
		register(skills, ".NET", ".net", "dotnet");
		register(skills, "MySQL", "mysql");
		register(skills, "SQL", "sql");
		return Map.copyOf(skills);
	}

	private static void register(Map<String, String> skills, String canonical, String... aliases) {
		for (String alias : aliases) {
			skills.put(comparisonKey(alias), canonical);
		}
	}
}
