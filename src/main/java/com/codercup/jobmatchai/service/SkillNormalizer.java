package com.codercup.jobmatchai.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
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
	private static final Map<String, List<String>> CANONICAL_SKILL_ALIASES = buildCanonicalSkillAliases();
	private static final Map<String, String> CANONICAL_SKILLS = buildCanonicalSkills(CANONICAL_SKILL_ALIASES);

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

	public static List<String> canonicalSkills() {
		return List.copyOf(new LinkedHashSet<>(CANONICAL_SKILLS.values()));
	}

	public static boolean containsCanonicalSkill(String text, String canonicalSkill) {
		String canonical = canonicalizeSkill(canonicalSkill);
		if (!equivalentSkill(canonical, canonicalSkill)) {
			return false;
		}
		String value = text == null ? "" : text;
		return CANONICAL_SKILL_ALIASES.getOrDefault(canonical, List.of()).stream()
				.anyMatch(alias -> matchesAlias(value, canonical, alias));
	}

	private static Map<String, List<String>> buildCanonicalSkillAliases() {
		Map<String, List<String>> aliases = new LinkedHashMap<>();
		register(aliases, "Java", "java");
		register(aliases, "JavaScript", "javascript", "js");
		register(aliases, "TypeScript", "typescript", "ts");
		register(aliases, "Node.js", "nodejs", "node.js", "node js");
		register(aliases, "PostgreSQL", "postgresql", "postgres");
		register(aliases, "Vue.js", "vue", "vuejs", "vue.js");
		register(aliases, "React", "react", "reactjs", "react.js");
		register(aliases, "Spring Boot", "spring boot", "springboot", "spring-boot");
		register(aliases, "REST APIs", "rest api", "rest apis", "restful api", "restful apis",
				"restful service", "restful services");
		register(aliases, "Docker", "docker");
		register(aliases, "Kubernetes", "kubernetes", "k8s");
		register(aliases, "C#", "c#", "csharp");
		register(aliases, ".NET", ".net", "dotnet");
		register(aliases, "MySQL", "mysql");
		register(aliases, "SQL", "sql");
		register(aliases, "AWS", "aws", "amazon web services");
		register(aliases, "Azure", "azure", "microsoft azure");
		register(aliases, "GCP", "gcp", "google cloud", "google cloud platform");
		register(aliases, "Git", "git");
		register(aliases, "GitHub", "github");
		register(aliases, "GitLab", "gitlab");
		register(aliases, "JUnit", "junit");
		register(aliases, "Mockito", "mockito");
		register(aliases, "Testing", "testing", "tests", "test automation", "automated testing");
		register(aliases, "CI/CD", "ci/cd", "ci cd", "continuous integration", "continuous delivery");
		register(aliases, "GitHub Actions", "github actions");
		register(aliases, "Jenkins", "jenkins");
		register(aliases, "Linux", "linux");
		register(aliases, "Redis", "redis");
		register(aliases, "MongoDB", "mongodb", "mongo db");
		register(aliases, "Kafka", "kafka", "apache kafka");
		register(aliases, "RabbitMQ", "rabbitmq", "rabbit mq");
		register(aliases, "Microservices", "microservices", "microservice", "micro-services");
		register(aliases, "Maven", "maven");
		register(aliases, "Gradle", "gradle");
		register(aliases, "Hibernate", "hibernate");
		register(aliases, "JPA", "jpa", "java persistence api");
		register(aliases, "Angular", "angular");
		register(aliases, "Python", "python");
		register(aliases, "Django", "django");
		register(aliases, "FastAPI", "fastapi", "fast api");
		register(aliases, "Flask", "flask");
		register(aliases, "Express", "express", "express.js", "expressjs");
		register(aliases, "NestJS", "nestjs", "nest js", "nest.js");
		register(aliases, "Next.js", "nextjs", "next js", "next.js");
		register(aliases, "HTML", "html");
		register(aliases, "CSS", "css");
		register(aliases, "Sass", "sass", "scss");
		register(aliases, "Tailwind CSS", "tailwind css", "tailwind");
		return Collections.unmodifiableMap(aliases);
	}

	private static Map<String, String> buildCanonicalSkills(Map<String, List<String>> aliases) {
		Map<String, String> skills = new LinkedHashMap<>();
		aliases.forEach((canonical, values) -> values.forEach(alias -> skills.put(comparisonKey(alias), canonical)));
		return Collections.unmodifiableMap(skills);
	}

	private static void register(Map<String, List<String>> skills, String canonical, String... aliases) {
		skills.put(canonical, List.of(aliases));
	}

	private static boolean matchesAlias(String text, String canonical, String alias) {
		String normalized = normalizeDetectionText(text);
		String aliasPattern = normalizeDetectionText(alias).chars()
				.mapToObj(character -> Character.isLetterOrDigit(character)
						? Pattern.quote(String.valueOf((char) character))
						: "[^a-z0-9]+")
				.reduce("", String::concat);
		if ("React".equals(canonical)) {
			aliasPattern = aliasPattern + "(?!\\s+native)";
		}
		return Pattern.compile("(^|[^a-z0-9])" + aliasPattern + "([^a-z0-9]|$)")
				.matcher(normalized)
				.find();
	}

	private static String normalizeDetectionText(String raw) {
		String withoutAccents = Normalizer.normalize(raw.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
		String normalized = DIACRITICS_PATTERN.matcher(withoutAccents).replaceAll("");
		return normalized.replaceAll("\\s+", " ");
	}
}
