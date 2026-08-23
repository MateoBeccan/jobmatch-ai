package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProfessionalKnowledgeCatalogTest {

	@Test
	void containsCanonicalNamesAliasesDomainsAndTypes() {
		ProfessionalKnowledgeEntry excel = ProfessionalKnowledgeCatalog.findByCanonicalName("Microsoft Excel").orElseThrow();
		ProfessionalKnowledgeEntry springBoot = ProfessionalKnowledgeCatalog.findByAlias("springboot").orElseThrow();
		ProfessionalKnowledgeEntry bankReconciliation = ProfessionalKnowledgeCatalog.findByAlias("conciliaciones bancarias").orElseThrow();

		assertThat(excel.canonicalName()).isEqualTo("Microsoft Excel");
		assertThat(excel.aliases()).contains("microsoft excel", "ms excel", "manejo de excel");
		assertThat(excel.domains()).contains(
				ProfessionalDomain.ACCOUNTING_FINANCE,
				ProfessionalDomain.ADMINISTRATION,
				ProfessionalDomain.DATA_ANALYTICS,
				ProfessionalDomain.SALES,
				ProfessionalDomain.HUMAN_RESOURCES
		);
		assertThat(excel.type()).isEqualTo(KnowledgeType.TOOL);
		assertThat(springBoot.canonicalName()).isEqualTo("Spring Boot");
		assertThat(springBoot.type()).isEqualTo(KnowledgeType.TECHNOLOGY);
		assertThat(bankReconciliation.canonicalName()).isEqualTo("Bank Reconciliation");
		assertThat(bankReconciliation.type()).isEqualTo(KnowledgeType.BUSINESS_PROCESS);
	}

	@Test
	void hasNoCanonicalDuplicatesOrDangerousAliasCollisions() {
		Set<String> canonicalKeys = new HashSet<>();
		Map<String, String> aliasOwners = new LinkedHashMap<>();

		for (ProfessionalKnowledgeEntry entry : ProfessionalKnowledgeCatalog.allEntries()) {
			assertThat(canonicalKeys.add(ProfessionalKnowledgeCatalog.comparisonKey(entry.canonicalName()))).isTrue();
			for (String alias : entry.aliases()) {
				String previousOwner = aliasOwners.putIfAbsent(
						ProfessionalKnowledgeCatalog.comparisonKey(alias),
						entry.canonicalName()
				);
				assertThat(previousOwner == null || previousOwner.equals(entry.canonicalName()))
						.as("alias collision for " + alias)
						.isTrue();
			}
		}
	}

	@Test
	void exposesImmutableCollections() {
		ProfessionalKnowledgeEntry excel = ProfessionalKnowledgeCatalog.findByCanonicalName("Microsoft Excel").orElseThrow();

		assertThatThrownBy(() -> ProfessionalKnowledgeCatalog.allEntries().add(excel))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> excel.aliases().add("excel"))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> excel.domains().add(ProfessionalDomain.GENERAL))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void retrievesEntriesByDomainWithoutMakingDomainExclusive() {
		List<ProfessionalKnowledgeEntry> accounting = ProfessionalKnowledgeCatalog.entriesByDomain(
				ProfessionalDomain.ACCOUNTING_FINANCE
		);
		List<ProfessionalKnowledgeEntry> administration = ProfessionalKnowledgeCatalog.entriesByDomain(
				ProfessionalDomain.ADMINISTRATION
		);

		assertThat(accounting).extracting(ProfessionalKnowledgeEntry::canonicalName)
				.contains("Microsoft Excel", "Bank Reconciliation", "Payroll", "SAP");
		assertThat(administration).extracting(ProfessionalKnowledgeEntry::canonicalName)
				.contains("Microsoft Excel", "Document Management", "Inventory Management", "SAP");
	}

	@Test
	void technicalMarketSubsetKeepsCareerMarketScopeSeparate() {
		assertThat(ProfessionalKnowledgeCatalog.technicalMarketCanonicalNames())
				.contains("Java", "Spring Boot", "SQL", "Docker", "Tailwind CSS")
				.doesNotContain("Microsoft Excel", "Payroll", "Customer Service", "Bank Reconciliation");
		assertThat(ProfessionalKnowledgeCatalog.technicalMarketCanonicalNames()).hasSize(49);
	}

	@Test
	void normalizesProfessionalKnowledgeWithoutAffectingTechnicalSubset() {
		assertThat(ProfessionalKnowledgeCatalog.normalizeProfessionalKnowledgeList(List.of(
				"ms excel",
				"conciliacion bancaria",
				"springboot",
				"microsoft excel"
		))).containsExactly("Microsoft Excel", "Bank Reconciliation", "Spring Boot");

		assertThat(SkillNormalizer.canonicalSkills()).doesNotContain("Microsoft Excel");
		assertThat(SkillNormalizer.isCanonicalSkill("ms excel")).isFalse();
	}
}
