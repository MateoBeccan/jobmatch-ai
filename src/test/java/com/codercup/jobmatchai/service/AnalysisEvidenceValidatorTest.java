package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.codercup.jobmatchai.dto.JobSeniority;
import com.codercup.jobmatchai.dto.internal.GeminiAnalysisResult;
import com.codercup.jobmatchai.dto.internal.GeminiJobSearchProfile;
import com.codercup.jobmatchai.scoring.RequirementAssessment;
import com.codercup.jobmatchai.scoring.RequirementCategory;
import com.codercup.jobmatchai.scoring.RequirementStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisEvidenceValidatorTest {

	private final AnalysisEvidenceValidator validator = new AnalysisEvidenceValidator();
	private final ProfessionalKnowledgeExtractor extractor = new ProfessionalKnowledgeExtractor();

	@Test
	void removesKnownMatchingSkillsNotBackedByCvButKeepsUnknownKnowledge() {
		GeminiAnalysisResult validated = validate(
				result(
						List.of(requirement("Java", RequirementStatus.MATCH)),
						List.of("Java", "Docker", "Oracle NetSuite"),
						List.of()
				),
				"Java, Spring Boot",
				"Java and Docker",
				true
		);

		assertThat(validated.matchingSkills()).containsExactly("Java", "Oracle NetSuite");
	}

	@Test
	void validatesNonItMatchingSkillsAgainstCv() {
		GeminiAnalysisResult validated = validate(
				result(
						List.of(requirement("Microsoft Excel", RequirementStatus.MATCH)),
						List.of("Microsoft Excel", "SAP"),
						List.of()
				),
				"Experiencia con Microsoft Excel y conciliaciones bancarias.",
				"Microsoft Excel and SAP required.",
				true
		);

		assertThat(validated.matchingSkills()).containsExactly("Microsoft Excel");
	}

	@Test
	void removesMissingSkillsAlreadyDetectedInCv() {
		GeminiAnalysisResult validated = validate(
				result(
						List.of(requirement("Docker", RequirementStatus.MISSING)),
						List.of(),
						List.of("Docker", "AWS")
				),
				"Docker",
				"Docker and AWS",
				true
		);

		assertThat(validated.missingSkills()).containsExactly("AWS");
	}

	@Test
	void removesKnownMissingSkillsNotDetectedInTextualOffer() {
		GeminiAnalysisResult validated = validate(
				result(
						List.of(requirement("Docker", RequirementStatus.MISSING)),
						List.of(),
						List.of("Docker")
				),
				"Java",
				"Java, Spring Boot and SQL.",
				true
		);

		assertThat(validated.missingSkills()).isEmpty();
	}

	@Test
	void keepsKnownMissingSkillsForImageOfferBecauseThereIsNoTextualJobEvidence() {
		GeminiAnalysisResult validated = validate(
				result(
						List.of(requirement("Docker", RequirementStatus.MISSING)),
						List.of(),
						List.of("Docker")
				),
				"Java",
				"",
				false
		);

		assertThat(validated.missingSkills()).containsExactly("Docker");
	}

	@Test
	void validatesAtomicRequirementsWhenCvContainsKnowledge() {
		GeminiAnalysisResult validated = validate(
				result(
						List.of(
								requirement("Docker", RequirementStatus.MISSING),
								requirement("Docker", RequirementStatus.PARTIAL),
								requirement("Docker", RequirementStatus.MATCH)
						),
						List.of(),
						List.of()
				),
				"Docker",
				"Docker",
				true
		);

		assertThat(validated.requirements()).extracting(RequirementAssessment::status)
				.containsExactly(RequirementStatus.PARTIAL, RequirementStatus.PARTIAL, RequirementStatus.MATCH);
	}

	@Test
	void validatesAtomicRequirementsWhenCvDoesNotContainKnowledge() {
		GeminiAnalysisResult validated = validate(
				result(
						List.of(
								requirement("Docker", RequirementStatus.MATCH),
								requirement("Docker", RequirementStatus.PARTIAL),
								requirement("Docker", RequirementStatus.MISSING)
						),
						List.of(),
						List.of("Docker")
				),
				"Java and SQL",
				"Docker",
				true
		);

		assertThat(validated.requirements()).extracting(RequirementAssessment::status)
				.containsExactly(RequirementStatus.MISSING, RequirementStatus.MISSING, RequirementStatus.MISSING);
	}

	@Test
	void doesNotModifyExperienceOrComplexRequirements() {
		List<RequirementAssessment> requirements = List.of(
				requirement("3+ years Docker", RequirementCategory.EXPERIENCE_SENIORITY, RequirementStatus.MATCH),
				requirement("Java or Kotlin", RequirementStatus.MATCH),
				requirement("Java and Spring Boot", RequirementStatus.PARTIAL),
				requirement("PostgreSQL or equivalent relational database", RequirementStatus.MATCH),
				requirement("Advanced Excel with professional experience", RequirementStatus.MISSING)
		);

		GeminiAnalysisResult validated = validate(
				result(requirements, List.of(), List.of()),
				"Java, Spring Boot, PostgreSQL, Microsoft Excel and Docker.",
				"Java or Kotlin. Advanced Excel with professional experience.",
				true
		);

		assertThat(validated.requirements()).isEqualTo(requirements);
	}

	@Test
	void validatesAccountingEvidenceConservatively() {
		GeminiAnalysisResult validated = validate(
				result(
						List.of(
								requirement("Bank Reconciliation", RequirementStatus.MATCH),
								requirement("Microsoft Excel", RequirementStatus.MISSING),
								requirement("SAP", RequirementStatus.MISSING)
						),
						List.of("Bank Reconciliation", "Microsoft Excel", "SAP"),
						List.of("Microsoft Excel", "SAP")
				),
				"Conciliaciones bancarias y Microsoft Excel.",
				"Microsoft Excel, Bank Reconciliation and SAP.",
				true
		);

		assertThat(validated.requirements()).extracting(RequirementAssessment::status)
				.containsExactly(RequirementStatus.MATCH, RequirementStatus.PARTIAL, RequirementStatus.MISSING);
		assertThat(validated.matchingSkills()).containsExactly("Bank Reconciliation", "Microsoft Excel");
		assertThat(validated.missingSkills()).containsExactly("SAP");
	}

	@Test
	void validatesAdministrationAndCustomerServiceEvidence() {
		GeminiAnalysisResult administration = validate(
				result(
						List.of(
								requirement("Document Management", RequirementStatus.MISSING),
								requirement("Data Entry", RequirementStatus.MATCH)
						),
						List.of("Document Management", "Data Entry", "Inventory Management"),
						List.of()
				),
				"Gestion documental y carga de datos.",
				"Document Management, Data Entry and Inventory Management.",
				true
		);
		GeminiAnalysisResult customerService = validate(
				result(
						List.of(
								requirement("Customer Service", RequirementStatus.MISSING),
								requirement("Complaint Handling", RequirementStatus.MATCH)
						),
						List.of("Customer Service", "Complaint Handling", "CRM"),
						List.of()
				),
				"Atencion al cliente y manejo de reclamos.",
				"Customer Service, Complaint Handling and CRM.",
				true
		);

		assertThat(administration.requirements()).extracting(RequirementAssessment::status)
				.containsExactly(RequirementStatus.PARTIAL, RequirementStatus.MATCH);
		assertThat(administration.matchingSkills()).containsExactly("Document Management", "Data Entry");
		assertThat(customerService.requirements()).extracting(RequirementAssessment::status)
				.containsExactly(RequirementStatus.PARTIAL, RequirementStatus.MATCH);
		assertThat(customerService.matchingSkills()).containsExactly("Customer Service", "Complaint Handling");
	}

	@Test
	void validatesJobSearchProfileKeywordsAndBackfillsFromCvOnly() {
		GeminiAnalysisResult validated = validate(
				result(
						List.of(requirement("Java", RequirementStatus.MATCH)),
						List.of("Java"),
						List.of(),
						List.of("Java", "AWS", "Spring Boot")
				),
				"Java, Spring Boot and SQL.",
				"Java, AWS and Spring Boot.",
				true
		);

		assertThat(validated.jobSearchProfile().keywords()).containsExactly("Java", "Spring Boot", "SQL");
	}

	@Test
	void allowsEvidenceValidationToReturnFewerThanThreeKeywordsInsteadOfInventing() {
		GeminiAnalysisResult validated = validate(
				result(
						List.of(requirement("Java", RequirementStatus.MATCH)),
						List.of("Java"),
						List.of(),
						List.of("Java", "AWS", "Docker")
				),
				"Java and SQL.",
				"Java, AWS and Docker.",
				true
		);

		assertThat(validated.jobSearchProfile().keywords()).containsExactly("Java", "SQL");
	}

	@Test
	void allowsNonItKeywordValidationToReturnFewerThanThreeKeywordsInsteadOfInventing() {
		GeminiAnalysisResult validated = validate(
				result(
						List.of(requirement("Microsoft Excel", RequirementStatus.MATCH)),
						List.of("Microsoft Excel"),
						List.of(),
						List.of("Microsoft Excel", "SAP", "Payroll")
				),
				"Microsoft Excel.",
				"Microsoft Excel, SAP and Payroll.",
				true
		);

		assertThat(validated.jobSearchProfile().keywords()).containsExactly("Microsoft Excel");
	}

	@Test
	void preservesUnknownJobSearchProfileKeywordsWhenCatalogCannotValidateThem() {
		GeminiAnalysisResult validated = validate(
				result(
						List.of(requirement("Financial Analysis", RequirementStatus.MATCH)),
						List.of("Financial Analysis"),
						List.of(),
						List.of("Oracle NetSuite", "Financial Analysis", "ERP")
				),
				"General professional profile without catalog matches.",
				"Oracle NetSuite, Financial Analysis and ERP.",
				true
		);

		assertThat(validated.jobSearchProfile().keywords())
				.containsExactly("Oracle NetSuite", "Financial Analysis", "ERP");
	}

	@Test
	void validatesNonItJobSearchProfileKeywords() {
		GeminiAnalysisResult validated = validate(
				result(
						List.of(requirement("Microsoft Excel", RequirementStatus.MATCH)),
						List.of("Microsoft Excel"),
						List.of(),
						List.of("Microsoft Excel", "SAP", "Bank Reconciliation")
				),
				"Microsoft Excel, conciliaciones bancarias y facturacion.",
				"Microsoft Excel, SAP and Bank Reconciliation.",
				true
		);

		assertThat(validated.jobSearchProfile().keywords())
				.containsExactly("Microsoft Excel", "Bank Reconciliation", "Invoicing");
	}

	private GeminiAnalysisResult validate(
			GeminiAnalysisResult result,
			String cvText,
			String jobText,
			boolean hasTextualJobEvidence
	) {
		return validator.validate(
				result,
				extractor.extract(cvText),
				hasTextualJobEvidence ? extractor.extract(jobText) : List.of(),
				hasTextualJobEvidence
		);
	}

	private GeminiAnalysisResult result(
			List<RequirementAssessment> requirements,
			List<String> matchingSkills,
			List<String> missingSkills
	) {
		return result(requirements, matchingSkills, missingSkills, List.of("Java", "Spring Boot", "SQL"));
	}

	private GeminiAnalysisResult result(
			List<RequirementAssessment> requirements,
			List<String> matchingSkills,
			List<String> missingSkills,
			List<String> keywords
	) {
		return new GeminiAnalysisResult(
				requirements,
				matchingSkills,
				missingSkills,
				List.of("Recomendacion uno", "Recomendacion dos"),
				List.of("Pregunta uno", "Pregunta dos", "Pregunta tres"),
				new GeminiJobSearchProfile("Backend Developer", JobSeniority.JUNIOR, keywords)
		);
	}

	private RequirementAssessment requirement(String name, RequirementStatus status) {
		return requirement(name, RequirementCategory.MANDATORY_TECHNICAL, status);
	}

	private RequirementAssessment requirement(
			String name,
			RequirementCategory category,
			RequirementStatus status
	) {
		return new RequirementAssessment(name, category, status, "Evidence fixture");
	}
}
