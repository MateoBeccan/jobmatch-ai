package com.codercup.jobmatchai.evaluation;

import static com.codercup.jobmatchai.scoring.RequirementCategory.COMPLEMENTARY;
import static com.codercup.jobmatchai.scoring.RequirementCategory.DESIRABLE;
import static com.codercup.jobmatchai.scoring.RequirementCategory.EXPERIENCE_SENIORITY;
import static com.codercup.jobmatchai.scoring.RequirementCategory.MANDATORY_TECHNICAL;
import static com.codercup.jobmatchai.scoring.RequirementCriticality.CRITICAL;
import static com.codercup.jobmatchai.scoring.RequirementCriticality.NORMAL;
import static com.codercup.jobmatchai.scoring.RequirementStatus.MATCH;
import static com.codercup.jobmatchai.scoring.RequirementStatus.MISSING;
import static com.codercup.jobmatchai.scoring.RequirementStatus.PARTIAL;
import static com.codercup.jobmatchai.service.ProfessionalDomain.ACCOUNTING_FINANCE;
import static com.codercup.jobmatchai.service.ProfessionalDomain.ADMINISTRATION;
import static com.codercup.jobmatchai.service.ProfessionalDomain.CUSTOMER_SERVICE;
import static com.codercup.jobmatchai.service.ProfessionalDomain.DATA_ANALYTICS;
import static com.codercup.jobmatchai.service.ProfessionalDomain.HUMAN_RESOURCES;
import static com.codercup.jobmatchai.service.ProfessionalDomain.OPERATIONS;
import static com.codercup.jobmatchai.service.ProfessionalDomain.SALES;
import static com.codercup.jobmatchai.service.ProfessionalDomain.SOFTWARE_DEVELOPMENT;

import com.codercup.jobmatchai.dto.JobSeniority;
import com.codercup.jobmatchai.dto.internal.GeminiAnalysisResult;
import com.codercup.jobmatchai.dto.internal.GeminiJobSearchProfile;
import com.codercup.jobmatchai.scoring.RequirementAssessment;
import com.codercup.jobmatchai.scoring.RequirementCategory;
import com.codercup.jobmatchai.scoring.RequirementCriticality;
import com.codercup.jobmatchai.scoring.RequirementStatus;
import com.codercup.jobmatchai.service.ProfessionalDomain;
import java.util.List;
import java.util.Set;

final class GoldenCaseFixtures {

	private GoldenCaseFixtures() {
	}

	static List<GoldenAnalysisCase> allCases() {
		return List.of(
				c("GOLD-IT-001", SOFTWARE_DEVELOPMENT, "Junior Java aligned",
						"Protect a high-match junior Java scenario.",
						"Backend profile with Java, Spring Boot, SQL, Git and REST APIs in junior projects.",
						"Junior backend role requiring Java, Spring Boot, SQL, Git and REST APIs.",
						List.of(r("Java", MANDATORY_TECHNICAL, MATCH), r("Spring Boot", MANDATORY_TECHNICAL, MATCH),
								r("SQL", MANDATORY_TECHNICAL, MATCH), r("Git", COMPLEMENTARY, MATCH),
								r("REST APIs", COMPLEMENTARY, MATCH)),
						List.of("Java", "Spring Boot", "SQL", "Git", "REST APIs"), List.of(), 85, 100,
						tags("HIGH_MATCH", "EXACT_REQUIRED")),
				c("GOLD-IT-002", SOFTWARE_DEVELOPMENT, "Junior Java vs senior cloud",
						"Ensure junior evidence does not satisfy senior cloud expectations.",
						"Junior backend profile with Java, Spring Boot and SQL. No senior cloud production experience.",
						"Senior Java role requiring 5+ years professional Java, AWS and Kubernetes.",
						List.of(r("Java", MANDATORY_TECHNICAL, MATCH), r("AWS", MANDATORY_TECHNICAL, MISSING),
								r("Kubernetes", MANDATORY_TECHNICAL, MISSING),
								r("5+ years professional Java experience", EXPERIENCE_SENIORITY, CRITICAL, MISSING)),
						List.of("Java"), List.of("AWS", "Kubernetes", "5+ years professional Java experience"),
						0, 69, tags("LOW_MATCH", "EXPERIENCE_GAP")),
				c("GOLD-IT-003", SOFTWARE_DEVELOPMENT, "Vue accepted by OR",
						"Ensure alternatives stay semantic and are not split into atomic requirements.",
						"Frontend profile with Vue, TypeScript and REST APIs.",
						"Frontend role requiring React or Vue and TypeScript.",
						List.of(r("React or Vue", MANDATORY_TECHNICAL, MATCH),
								r("TypeScript", MANDATORY_TECHNICAL, MATCH)),
						List.of("Vue.js", "TypeScript"), List.of(), 85, 100,
						tags("HIGH_MATCH", "ALTERNATIVE_TECH")),
				c("GOLD-IT-004", SOFTWARE_DEVELOPMENT, "Vue vs React required",
						"Ensure Vue does not satisfy an exact React requirement.",
						"Frontend profile with Vue and TypeScript.",
						"Frontend role requiring React specifically and TypeScript.",
						List.of(r("React", MANDATORY_TECHNICAL, MISSING),
								r("TypeScript", MANDATORY_TECHNICAL, MATCH)),
						List.of("TypeScript", "Vue.js"), List.of("React"), 0, 69,
						tags("LOW_MATCH", "EXACT_REQUIRED")),
				c("GOLD-IT-005", SOFTWARE_DEVELOPMENT, "MySQL vs relational equivalent",
						"Track equivalent database wording without hard-coding semantic equivalence.",
						"Backend profile with MySQL, SQL and relational data modeling.",
						"Backend role requiring PostgreSQL or equivalent relational database plus SQL.",
						List.of(r("PostgreSQL or equivalent relational database", MANDATORY_TECHNICAL, PARTIAL),
								r("SQL", MANDATORY_TECHNICAL, MATCH)),
						List.of("MySQL", "SQL"), List.of(), 70, 85,
						tags("MEDIUM_MATCH", "EQUIVALENT_WORDING")),
				c("GOLD-IT-006", SOFTWARE_DEVELOPMENT, "Academic Java vs professional years",
						"Ensure academic Java exposure does not satisfy 3+ years professional experience.",
						"Academic coursework with Java and a class API project, no professional Java employment.",
						"Java role requiring Java and 3+ years professional Java experience.",
						List.of(r("Java", MANDATORY_TECHNICAL, MATCH),
								r("3+ years professional Java experience", EXPERIENCE_SENIORITY, CRITICAL, MISSING)),
						List.of("Java"), List.of("3+ years professional Java experience"), 0, 69,
						tags("LOW_MATCH", "ACADEMIC_VS_PROFESSIONAL")),
				c("GOLD-IT-007", SOFTWARE_DEVELOPMENT, "Java 21 vs Java 17+",
						"Track version-compatible Java wording.",
						"Backend profile with Java 21 and Spring Boot production projects.",
						"Backend role requiring Java 17+ and Spring Boot.",
						List.of(r("Java 17+", MANDATORY_TECHNICAL, MATCH),
								r("Spring Boot", MANDATORY_TECHNICAL, MATCH)),
						List.of("Java 21", "Spring Boot"), List.of(), 85, 100,
						tags("HIGH_MATCH", "EQUIVALENT_WORDING")),
				c("GOLD-IT-008", SOFTWARE_DEVELOPMENT, "Docker mandatory correction",
						"Ensure Evidence Validation removes invented Docker match when Docker is mandatory.",
						"Backend profile with Java and SQL, no Docker.",
						"Backend role requiring Java and Docker as mandatory skills.",
						List.of(r("Java", MANDATORY_TECHNICAL, MATCH), r("Docker", MANDATORY_TECHNICAL, MISSING)),
						List.of("Java"), List.of("Docker"), 0, 69, tags("LOW_MATCH", "NEGATION"),
						result(List.of(r("Java", MANDATORY_TECHNICAL, MATCH), r("Docker", MANDATORY_TECHNICAL, MATCH)),
								List.of("Java", "Docker"), List.of("Docker"))),

				c("GOLD-DATA-001", DATA_ANALYTICS, "Data analyst aligned",
						"Protect a high-match SQL, Python and Power BI data case.",
						"Data profile with Python, SQL, Pandas, Power BI and reporting projects.",
						"Data Analyst role requiring SQL, Python, Pandas and Power BI.",
						List.of(r("SQL", MANDATORY_TECHNICAL, MATCH), r("Python", MANDATORY_TECHNICAL, MATCH),
								r("Pandas", MANDATORY_TECHNICAL, MATCH), r("Power BI", COMPLEMENTARY, MATCH)),
						List.of("SQL", "Python", "Pandas", "Power BI"), List.of(), 85, 100,
						tags("HIGH_MATCH")),
				c("GOLD-DATA-002", DATA_ANALYTICS, "Excel SQL vs BI stack",
						"Measure partial data alignment without inventing Python or Tableau.",
						"Analytics support profile with Microsoft Excel and SQL dashboards.",
						"Data Analyst role requiring SQL, Python, Tableau and Power BI.",
						List.of(r("SQL", MANDATORY_TECHNICAL, MATCH), r("Python", MANDATORY_TECHNICAL, MISSING),
								r("Tableau", DESIRABLE, MISSING), r("Power BI", DESIRABLE, MISSING)),
						List.of("SQL", "Microsoft Excel"), List.of("Python", "Tableau", "Power BI"), 40, 75,
						tags("MEDIUM_MATCH", "NON_IT")),
				c("GOLD-DATA-003", DATA_ANALYTICS, "Senior data analyst gap",
						"Ensure senior years and absent Python/SQL remain visible.",
						"Reporting profile with Power BI and Microsoft Excel dashboards.",
						"Senior Data Analyst role requiring 4+ years, Python, SQL and Power BI.",
						List.of(r("Power BI", MANDATORY_TECHNICAL, MATCH), r("Python", MANDATORY_TECHNICAL, MISSING),
								r("SQL", MANDATORY_TECHNICAL, MISSING),
								r("4+ years data analysis experience", EXPERIENCE_SENIORITY, CRITICAL, MISSING)),
						List.of("Power BI", "Microsoft Excel"), List.of("Python", "SQL", "4+ years data analysis experience"),
						0, 69, tags("LOW_MATCH", "EXPERIENCE_GAP")),
				c("GOLD-DATA-004", DATA_ANALYTICS, "Tableau or Power BI alternative",
						"Track OR wording for business intelligence tools.",
						"Analytics profile with Tableau, SQL and stakeholder reporting.",
						"Data role requiring Tableau or Power BI and SQL.",
						List.of(r("Tableau or Power BI", MANDATORY_TECHNICAL, MATCH),
								r("SQL", MANDATORY_TECHNICAL, MATCH)),
						List.of("Tableau", "SQL"), List.of(), 85, 100, tags("HIGH_MATCH", "ALTERNATIVE_TECH")),

				c("GOLD-ACC-001", ACCOUNTING_FINANCE, "Accounting assistant aligned",
						"Protect a high-match accounting assistant case.",
						"Accounting support profile with Microsoft Excel, Bank Reconciliation, Invoicing and Accounts Payable.",
						"Accounting Assistant role requiring Excel, bank reconciliations, invoicing and accounts payable.",
						List.of(r("Microsoft Excel", MANDATORY_TECHNICAL, MATCH),
								r("Bank Reconciliation", MANDATORY_TECHNICAL, MATCH),
								r("Invoicing", MANDATORY_TECHNICAL, MATCH),
								r("Accounts Payable", COMPLEMENTARY, MATCH)),
						List.of("Microsoft Excel", "Bank Reconciliation", "Invoicing", "Accounts Payable"),
						List.of(), 85, 100, tags("HIGH_MATCH", "NON_IT")),
				c("GOLD-ACC-002", ACCOUNTING_FINANCE, "Excel invoicing vs SAP reconciliation",
						"Ensure missingSkills cannot claim Excel absent when CV shows Excel.",
						"Finance assistant profile with Microsoft Excel and Invoicing.",
						"Accounting role requiring Microsoft Excel, SAP and Bank Reconciliation.",
						List.of(r("Microsoft Excel", MANDATORY_TECHNICAL, MATCH),
								r("SAP", MANDATORY_TECHNICAL, MISSING),
								r("Bank Reconciliation", MANDATORY_TECHNICAL, MISSING)),
						List.of("Microsoft Excel"), List.of("SAP", "Bank Reconciliation"), 30, 75,
						tags("MEDIUM_MATCH", "NON_IT"),
						result(List.of(r("Microsoft Excel", MANDATORY_TECHNICAL, MATCH),
										r("SAP", MANDATORY_TECHNICAL, MISSING),
										r("Bank Reconciliation", MANDATORY_TECHNICAL, MISSING)),
								List.of("Microsoft Excel"), List.of("Microsoft Excel", "SAP", "Bank Reconciliation"))),
				c("GOLD-ACC-003", ACCOUNTING_FINANCE, "Bookkeeping vs AP SAP",
						"Do not assume bookkeeping and QuickBooks satisfy specific SAP/AP requirements.",
						"Bookkeeping profile with QuickBooks and monthly records.",
						"Accounts Payable role requiring Accounts Payable process and SAP.",
						List.of(r("Accounts Payable", MANDATORY_TECHNICAL, MISSING),
								r("SAP", MANDATORY_TECHNICAL, MISSING),
								r("QuickBooks", DESIRABLE, MATCH)),
						List.of("QuickBooks", "Bookkeeping"), List.of("Accounts Payable", "SAP"), 0, 69,
						tags("LOW_MATCH", "EXACT_REQUIRED")),
				c("GOLD-ACC-004", ACCOUNTING_FINANCE, "Academic accounting vs professional years",
						"Ensure academic accounting coursework does not satisfy professional tenure.",
						"Academic accounting project with Microsoft Excel, no professional accounting employment.",
						"Accounting role requiring 2+ years professional accounting experience and Excel.",
						List.of(r("Microsoft Excel", MANDATORY_TECHNICAL, MATCH),
								r("2+ years professional accounting experience", EXPERIENCE_SENIORITY, CRITICAL, MISSING)),
						List.of("Microsoft Excel"), List.of("2+ years professional accounting experience"),
						0, 69, tags("LOW_MATCH", "ACADEMIC_VS_PROFESSIONAL")),
				c("GOLD-ACC-005", ACCOUNTING_FINANCE, "Advanced Excel wording",
						"Track proficiency level without upgrading plain Excel to mastery.",
						"Accounting profile with manejo de Microsoft Excel and Invoicing.",
						"Accounting role requiring Advanced Excel and invoicing.",
						List.of(r("Advanced Excel", MANDATORY_TECHNICAL, PARTIAL),
								r("Invoicing", MANDATORY_TECHNICAL, MATCH)),
						List.of("Microsoft Excel", "Invoicing"), List.of(), 70, 85,
						tags("MEDIUM_MATCH", "PROFICIENCY_LEVEL")),

				c("GOLD-ADM-001", ADMINISTRATION, "Administrative aligned",
						"Protect data entry and document management alignment.",
						"Administrative profile with Data Entry, Document Management and Microsoft Excel.",
						"Administrative assistant role requiring data entry, document management and Excel.",
						List.of(r("Data Entry", MANDATORY_TECHNICAL, MATCH),
								r("Document Management", MANDATORY_TECHNICAL, MATCH),
								r("Microsoft Excel", COMPLEMENTARY, MATCH)),
						List.of("Data Entry", "Document Management", "Microsoft Excel"), List.of(), 85, 100,
						tags("HIGH_MATCH", "NON_IT")),
				c("GOLD-ADM-002", ADMINISTRATION, "Inventory and purchase orders",
						"Measure operations-administration overlap with process evidence.",
						"Administrative operations profile with Inventory Management and Purchase Orders.",
						"Back office role requiring inventory management, purchase orders and procurement.",
						List.of(r("Inventory Management", MANDATORY_TECHNICAL, MATCH),
								r("Purchase Orders", MANDATORY_TECHNICAL, MATCH),
								r("Procurement", DESIRABLE, MISSING)),
						List.of("Inventory Management", "Purchase Orders"), List.of("Procurement"), 70, 90,
						tags("MEDIUM_MATCH", "TRANSFERABLE_SKILL")),
				c("GOLD-ADM-003", ADMINISTRATION, "Admin without Excel",
						"Ensure administrative support does not imply Excel when explicitly required.",
						"Administrative support profile with Document Management and Scheduling, no Microsoft Excel.",
						"Administrative role requiring Microsoft Excel and document management.",
						List.of(r("Microsoft Excel", MANDATORY_TECHNICAL, MISSING),
								r("Document Management", MANDATORY_TECHNICAL, MATCH)),
						List.of("Document Management", "Scheduling"), List.of("Microsoft Excel"), 0, 69,
						tags("LOW_MATCH", "NEGATION")),
				c("GOLD-ADM-004", ADMINISTRATION, "Office general vs specific tools",
						"Track general office support against specific tool requirements.",
						"Office support profile with Administrative Support and Scheduling.",
						"Office role requiring Administrative Support, Microsoft Outlook and Google Workspace.",
						List.of(r("Administrative Support", MANDATORY_TECHNICAL, MATCH),
								r("Microsoft Outlook", COMPLEMENTARY, MISSING),
								r("Google Workspace", COMPLEMENTARY, MISSING)),
						List.of("Administrative Support", "Scheduling"), List.of("Microsoft Outlook", "Google Workspace"),
						70, 90, tags("MEDIUM_MATCH", "EXACT_REQUIRED")),

				c("GOLD-CS-001", CUSTOMER_SERVICE, "Customer service aligned",
						"Protect customer service, CRM and complaints alignment.",
						"Customer service profile with Customer Service, CRM and Complaint Handling.",
						"Support role requiring customer service, CRM and complaint handling.",
						List.of(r("Customer Service", MANDATORY_TECHNICAL, MATCH),
								r("CRM", MANDATORY_TECHNICAL, MATCH),
								r("Complaint Handling", COMPLEMENTARY, MATCH)),
						List.of("Customer Service", "CRM", "Complaint Handling"), List.of(), 85, 100,
						tags("HIGH_MATCH", "NON_IT")),
				c("GOLD-CS-002", CUSTOMER_SERVICE, "Public service without CRM",
						"Ensure public-facing service does not imply CRM tool evidence.",
						"Atencion al publico profile with Complaint Handling, no CRM.",
						"Customer support role requiring Customer Service and CRM.",
						List.of(r("Customer Service", MANDATORY_TECHNICAL, MATCH),
								r("CRM", MANDATORY_TECHNICAL, MISSING)),
						List.of("Customer Service", "Complaint Handling"), List.of("CRM"), 0, 69,
						tags("LOW_MATCH", "NEGATION")),
				c("GOLD-CS-003", CUSTOMER_SERVICE, "POS commercial service",
						"Measure transferable retail service and point of sale evidence.",
						"Retail support profile with Customer Service, Cash Handling and Point of Sale.",
						"Commercial service role requiring Customer Service, Point of Sale and CRM desirable.",
						List.of(r("Customer Service", MANDATORY_TECHNICAL, MATCH),
								r("Point of Sale", MANDATORY_TECHNICAL, MATCH),
								r("CRM", DESIRABLE, MISSING)),
						List.of("Customer Service", "Point of Sale", "Cash Handling"), List.of("CRM"),
						70, 90, tags("MEDIUM_MATCH", "TRANSFERABLE_SKILL")),

				c("GOLD-SALES-001", SALES, "Sales CRM prospecting",
						"Protect aligned sales development evidence.",
						"Sales profile with Sales, CRM, Prospecting and Lead Generation.",
						"Sales development role requiring sales, CRM and prospecting.",
						List.of(r("Sales", MANDATORY_TECHNICAL, MATCH), r("CRM", MANDATORY_TECHNICAL, MATCH),
								r("Prospecting", MANDATORY_TECHNICAL, MATCH)),
						List.of("Sales", "CRM", "Prospecting", "Lead Generation"), List.of(), 85, 100,
						tags("HIGH_MATCH", "NON_IT")),
				c("GOLD-SALES-002", SALES, "B2C vs B2B required",
						"Do not assume B2C sales satisfies explicit B2B sales.",
						"Sales profile with B2C Sales, Customer Service and Point of Sale.",
						"B2B Sales role requiring B2B Sales, CRM and Prospecting.",
						List.of(r("B2B Sales", MANDATORY_TECHNICAL, MISSING),
								r("CRM", MANDATORY_TECHNICAL, MISSING),
								r("Prospecting", MANDATORY_TECHNICAL, MISSING)),
						List.of("B2C Sales", "Customer Service"), List.of("B2B Sales", "CRM", "Prospecting"),
						0, 69, tags("LOW_MATCH", "EXACT_REQUIRED")),

				c("GOLD-HR-001", HUMAN_RESOURCES, "HR assistant payroll",
						"Measure HR support with payroll and spreadsheet evidence.",
						"HR assistant profile with Administrative Support, Payroll and Microsoft Excel.",
						"HR Assistant role requiring administrative support, payroll and Excel.",
						List.of(r("Administrative Support", MANDATORY_TECHNICAL, MATCH),
								r("Payroll", MANDATORY_TECHNICAL, MATCH),
								r("Microsoft Excel", COMPLEMENTARY, MATCH)),
						List.of("Administrative Support", "Payroll", "Microsoft Excel"), List.of(), 85, 100,
						tags("HIGH_MATCH", "NON_IT")),
				c("GOLD-HR-002", HUMAN_RESOURCES, "Admin general vs payroll",
						"Do not infer HR payroll from general administrative work.",
						"Administrative profile with Document Management and Scheduling.",
						"HR assistant role requiring Payroll and Administrative Support.",
						List.of(r("Payroll", MANDATORY_TECHNICAL, MISSING),
								r("Administrative Support", MANDATORY_TECHNICAL, MISSING)),
						List.of("Document Management", "Scheduling"), List.of("Payroll"), 0, 69,
						tags("LOW_MATCH", "TRANSFERABLE_SKILL")),

				c("GOLD-OPS-001", OPERATIONS, "Operations inventory procurement",
						"Measure operations process overlap without requiring a tool.",
						"Operations profile with Inventory Management, Procurement and Purchase Orders.",
						"Operations coordinator role requiring inventory management, procurement and purchase orders.",
						List.of(r("Inventory Management", MANDATORY_TECHNICAL, MATCH),
								r("Procurement", MANDATORY_TECHNICAL, MATCH),
								r("Purchase Orders", COMPLEMENTARY, MATCH)),
						List.of("Inventory Management", "Procurement", "Purchase Orders"), List.of(), 85, 100,
						tags("HIGH_MATCH", "NON_IT")),
				c("GOLD-OPS-002", OPERATIONS, "Operations absent SAP",
						"Ensure process experience does not imply a specific absent tool.",
						"Operations profile with Inventory Management and Procurement, no SAP.",
						"Operations role requiring Inventory Management, SAP and Purchase Orders.",
						List.of(r("Inventory Management", MANDATORY_TECHNICAL, MATCH),
								r("SAP", MANDATORY_TECHNICAL, MISSING),
								r("Purchase Orders", DESIRABLE, MISSING)),
						List.of("Inventory Management", "Procurement"), List.of("SAP", "Purchase Orders"),
						40, 75, tags("MEDIUM_MATCH", "NEGATION"))
		);
	}

	private static GoldenAnalysisCase c(
			String id,
			ProfessionalDomain domain,
			String name,
			String purpose,
			String cvText,
			String jobDescription,
			List<GoldenRequirement> expectedRequirements,
			List<String> expectedMatchingSkills,
			List<String> expectedMissingSkills,
			int minExpectedScore,
			int maxExpectedScore,
			Set<String> tags
	) {
		return c(
				id,
				domain,
				name,
				purpose,
				cvText,
				jobDescription,
				expectedRequirements,
				expectedMatchingSkills,
				expectedMissingSkills,
				minExpectedScore,
				maxExpectedScore,
				tags,
				result(expectedRequirements, expectedMatchingSkills, expectedMissingSkills)
		);
	}

	private static GoldenAnalysisCase c(
			String id,
			ProfessionalDomain domain,
			String name,
			String purpose,
			String cvText,
			String jobDescription,
			List<GoldenRequirement> expectedRequirements,
			List<String> expectedMatchingSkills,
			List<String> expectedMissingSkills,
			int minExpectedScore,
			int maxExpectedScore,
			Set<String> tags,
			GeminiAnalysisResult modelResult
	) {
		return new GoldenAnalysisCase(
				id,
				domain,
				name,
				purpose,
				cvText,
				jobDescription,
				expectedRequirements,
				expectedMatchingSkills,
				expectedMissingSkills,
				expectedRequirements.stream()
						.filter(requirement -> requirement.criticality() == CRITICAL)
						.filter(requirement -> requirement.expectedStatus() == MISSING)
						.map(GoldenRequirement::name)
						.toList(),
				expectedRequirements.stream()
						.filter(requirement -> requirement.category() == EXPERIENCE_SENIORITY)
						.filter(requirement -> requirement.expectedStatus() == MISSING
								|| requirement.expectedStatus() == PARTIAL)
						.map(GoldenRequirement::name)
						.findFirst()
						.orElse(null),
				minExpectedScore,
				maxExpectedScore,
				tags,
				modelResult
		);
	}

	private static GoldenRequirement r(
			String name,
			RequirementCategory category,
			RequirementStatus status
	) {
		return r(name, category, NORMAL, status);
	}

	private static GoldenRequirement r(
			String name,
			RequirementCategory category,
			RequirementCriticality criticality,
			RequirementStatus status
	) {
		return new GoldenRequirement(name, category, criticality, status);
	}

	private static GeminiAnalysisResult result(
			List<GoldenRequirement> requirements,
			List<String> matchingSkills,
			List<String> missingSkills
	) {
		return result(
				requirements,
				matchingSkills,
				missingSkills,
				new GeminiJobSearchProfile("Golden Fixture Role", JobSeniority.JUNIOR, defaultKeywords(matchingSkills))
		);
	}

	private static GeminiAnalysisResult result(
			List<GoldenRequirement> requirements,
			List<String> matchingSkills,
			List<String> missingSkills,
			GeminiJobSearchProfile profile
	) {
		return new GeminiAnalysisResult(
				requirements.stream()
						.map(GoldenRequirement::toAssessment)
						.toList(),
				matchingSkills,
				missingSkills,
				List.of("Prioritize the validated gaps.", "Prepare evidence-backed examples."),
				List.of("Describe a relevant project.", "How did you validate the work?", "What would you improve?"),
				profile
		);
	}

	private static List<String> defaultKeywords(List<String> matchingSkills) {
		if (matchingSkills.size() >= 3) {
			return matchingSkills.stream().limit(6).toList();
		}
		return matchingSkills;
	}

	private static Set<String> tags(String... values) {
		return Set.of(values);
	}
}
