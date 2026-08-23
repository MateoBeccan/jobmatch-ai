package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProfessionalKnowledgeExtractorTest {

	private final ProfessionalKnowledgeExtractor extractor = new ProfessionalKnowledgeExtractor();

	@Test
	void detectsItKnowledgeWithStableOrderAndBoundaries() {
		assertThat(extractor.extractCanonicalNames("Desarrollo backend con Java, Spring Boot, MySQL y REST APIs."))
				.containsExactly("Java", "Spring Boot", "REST APIs", "MySQL");
	}

	@Test
	void detectsAccountingKnowledge() {
		assertThat(extractor.extractCanonicalNames("Conciliaciones bancarias, facturacion y Microsoft Excel."))
				.containsExactly("Microsoft Excel", "Bank Reconciliation", "Invoicing");
	}

	@Test
	void detectsAdministrationKnowledge() {
		assertThat(extractor.extractCanonicalNames("Gestion documental, carga de datos y control de stock."))
				.containsExactly("Data Entry", "Document Management", "Inventory Management");
	}

	@Test
	void detectsCustomerServiceKnowledge() {
		assertThat(extractor.extractCanonicalNames("Atencion al cliente, CRM y manejo de reclamos."))
				.containsExactly("Customer Service", "CRM", "Complaint Handling");
	}

	@Test
	void detectsDataKnowledge() {
		assertThat(extractor.extractCanonicalNames("Power BI, SQL y Python."))
				.containsExactly("SQL", "Python", "Power BI");
	}

	@Test
	void avoidsAmbiguousFalsePositives() {
		assertThat(extractor.extractCanonicalNames("The candidate can excel in customer-facing environments."))
				.doesNotContain("Microsoft Excel");
		assertThat(extractor.extractCanonicalNames("The word processing task was completed."))
				.doesNotContain("Microsoft Word");
		assertThat(extractor.extractCanonicalNames("Worked with several teams across the company."))
				.doesNotContain("Microsoft Teams");
		assertThat(extractor.extractCanonicalNames("Access to customer records was restricted."))
				.doesNotContain("Microsoft Access");
	}

	@Test
	void ignoresSimpleNegatedKnowledgeMentions() {
		assertThat(extractor.extractCanonicalNames("sin Docker"))
				.doesNotContain("Docker");
		assertThat(extractor.extractCanonicalNames("no Docker"))
				.doesNotContain("Docker");
		assertThat(extractor.extractCanonicalNames("without Kubernetes"))
				.doesNotContain("Kubernetes");
		assertThat(extractor.extractCanonicalNames("CV backend con Java y SQL, sin Spring Boot."))
				.containsExactly("Java", "SQL");
		assertThat(extractor.extractCanonicalNames("CV with Java, Spring Boot and junior backend experience, no Docker."))
				.containsExactly("Java", "Spring Boot");
		assertThat(extractor.extractCanonicalNames("Candidate works with Java without Kubernetes production exposure."))
				.containsExactly("Java");
	}

	@Test
	void doesNotTreatNotOnlyPhrasesAsNegatedKnowledge() {
		assertThat(extractor.extractCanonicalNames("Experiencia no solo Docker, tambien Kubernetes."))
				.contains("Docker", "Kubernetes");
		assertThat(extractor.extractCanonicalNames("Experiencia no sólo Docker, tambien Kubernetes."))
				.contains("Docker", "Kubernetes");
		assertThat(extractor.extractCanonicalNames("Experience not only Docker but also Kubernetes."))
				.contains("Docker", "Kubernetes");
	}

	@Test
	void scopesNegationToTheFollowingKnowledgeMention() {
		assertThat(extractor.extractCanonicalNames("Experiencia con Docker, no Kubernetes."))
				.containsExactly("Docker");
	}

	@Test
	void documentsComplexExperienceNegationLimitation() {
		assertThat(extractor.extractCanonicalNames(
				"no tengo experiencia profesional con Docker, pero lo utilice en proyectos"
		)).doesNotContain("Docker");
	}

	@Test
	void isAccentInsensitiveAndDoesNotReturnDuplicates() {
		assertThat(extractor.extractCanonicalNames(
				"Conciliacion bancaria y conciliaciones bancarias con emision de facturas y facturacion."
		)).containsExactly("Bank Reconciliation", "Invoicing");
	}
}
