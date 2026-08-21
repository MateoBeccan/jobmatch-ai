package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SkillNormalizerTest {

	@Test
	void canonicalizesSafeAliases() {
		assertThat(SkillNormalizer.equivalentSkill("Postgres", "PostgreSQL")).isTrue();
		assertThat(SkillNormalizer.equivalentSkill("NodeJS", "Node.js")).isTrue();
		assertThat(SkillNormalizer.equivalentSkill("JS", "JavaScript")).isTrue();
		assertThat(SkillNormalizer.equivalentSkill("TS", "TypeScript")).isTrue();
		assertThat(SkillNormalizer.equivalentSkill("Vue", "Vue.js")).isTrue();
		assertThat(SkillNormalizer.equivalentSkill("ReactJS", "React")).isTrue();
		assertThat(SkillNormalizer.equivalentSkill("SpringBoot", "Spring Boot")).isTrue();
		assertThat(SkillNormalizer.equivalentSkill("k8s", "Kubernetes")).isTrue();
		assertThat(SkillNormalizer.equivalentSkill("CSharp", "C#")).isTrue();
		assertThat(SkillNormalizer.equivalentSkill("dotnet", ".NET")).isTrue();
	}

	@Test
	void keepsUnsafeRelatedTechnologiesDistinct() {
		assertThat(SkillNormalizer.equivalentSkill("Spring", "Spring Boot")).isFalse();
		assertThat(SkillNormalizer.equivalentSkill("GitHub", "Git")).isFalse();
		assertThat(SkillNormalizer.equivalentSkill("SQL", "MySQL")).isFalse();
		assertThat(SkillNormalizer.equivalentSkill("Java", "JavaScript")).isFalse();
		assertThat(SkillNormalizer.equivalentSkill("AWS", "Azure")).isFalse();
	}

	@Test
	void comparisonKeyIgnoresSyntaxButKeepsDifferentConceptsApart() {
		assertThat(SkillNormalizer.comparisonKey(" Node.js ")).isEqualTo("nodejs");
		assertThat(SkillNormalizer.comparisonKey("node js")).isEqualTo("nodejs");
		assertThat(SkillNormalizer.comparisonKey("spring-boot")).isEqualTo("springboot");
		assertThat(SkillNormalizer.comparisonKey("C#")).isEqualTo("c#");
		assertThat(SkillNormalizer.comparisonKey(".NET")).isEqualTo("net");
	}

	@Test
	void normalizeSkillListPreservesFirstConceptualPositionAndUsesReadableCanonicalNames() {
		List<String> normalized = SkillNormalizer.normalizeSkillList(List.of(
				"Java",
				"Postgres",
				"Docker",
				"PostgreSQL",
				"NodeJS",
				"Node.js"
		));

		assertThat(normalized).containsExactly("Java", "PostgreSQL", "Docker", "Node.js");
	}

	@Test
	void canonicalizesRestApiVariants() {
		assertThat(SkillNormalizer.equivalentSkill("REST API", "RESTful APIs")).isTrue();
		assertThat(SkillNormalizer.canonicalizeSkill("RESTful services")).isEqualTo("REST APIs");
	}
}
