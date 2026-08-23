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

	@Test
	void canonicalizesCareerMarketAliases() {
		assertThat(SkillNormalizer.equivalentSkill("Continuous Integration", "CI/CD")).isTrue();
		assertThat(SkillNormalizer.equivalentSkill("Continuous Delivery", "CI/CD")).isTrue();
		assertThat(SkillNormalizer.equivalentSkill("Github Actions", "GitHub Actions")).isTrue();
		assertThat(SkillNormalizer.equivalentSkill("Micro-service", "Microservices")).isTrue();
		assertThat(SkillNormalizer.equivalentSkill("Java Persistence API", "JPA")).isTrue();
		assertThat(SkillNormalizer.equivalentSkill("Fast API", "FastAPI")).isTrue();
		assertThat(SkillNormalizer.equivalentSkill("SCSS", "Sass")).isTrue();
	}

	@Test
	void keepsCurrentItRegressionAliases() {
		assertThat(SkillNormalizer.canonicalizeSkill("nodejs")).isEqualTo("Node.js");
		assertThat(SkillNormalizer.canonicalizeSkill("postgres")).isEqualTo("PostgreSQL");
		assertThat(SkillNormalizer.canonicalizeSkill("vuejs")).isEqualTo("Vue.js");
		assertThat(SkillNormalizer.canonicalizeSkill("springboot")).isEqualTo("Spring Boot");
		assertThat(SkillNormalizer.canonicalizeSkill("k8s")).isEqualTo("Kubernetes");
		assertThat(SkillNormalizer.canonicalizeSkill("csharp")).isEqualTo("C#");
		assertThat(SkillNormalizer.canonicalizeSkill("dotnet")).isEqualTo(".NET");
		assertThat(SkillNormalizer.canonicalizeSkill("amazon web services")).isEqualTo("AWS");
		assertThat(SkillNormalizer.canonicalizeSkill("google cloud platform")).isEqualTo("GCP");
		assertThat(SkillNormalizer.canonicalizeSkill("restful api")).isEqualTo("REST APIs");
	}

	@Test
	void detectsCanonicalSkillsWithSafeBoundaries() {
		assertThat(SkillNormalizer.containsCanonicalSkill("We use JavaScript and NoSQL", "Java")).isFalse();
		assertThat(SkillNormalizer.containsCanonicalSkill("We use JavaScript and NoSQL", "SQL")).isFalse();
		assertThat(SkillNormalizer.containsCanonicalSkill("We use Java and SQL", "Java")).isTrue();
		assertThat(SkillNormalizer.containsCanonicalSkill("We use Java and SQL", "SQL")).isTrue();
		assertThat(SkillNormalizer.containsCanonicalSkill("K8s and Postgres", "Kubernetes")).isTrue();
		assertThat(SkillNormalizer.containsCanonicalSkill("K8s and Postgres", "PostgreSQL")).isTrue();
		assertThat(SkillNormalizer.containsCanonicalSkill("React Native mobile work", "React")).isFalse();
	}
}
