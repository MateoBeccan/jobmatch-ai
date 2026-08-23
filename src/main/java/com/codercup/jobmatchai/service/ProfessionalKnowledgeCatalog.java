package com.codercup.jobmatchai.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Central catalog of detectable professional knowledge.
 * It is intentionally broader than IT: entries can represent tools, processes,
 * methods, business knowledge or technologies. Detection is evidence only; it
 * does not prove experience level, equivalence, hiring probability or market demand.
 */
public final class ProfessionalKnowledgeCatalog {

	private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}");
	private static final Pattern COMPARISON_SEPARATOR_PATTERN = Pattern.compile("[^a-z0-9#+]+");
	private static final List<ProfessionalKnowledgeEntry> ENTRIES = buildEntries();
	private static final Set<String> TECHNICAL_MARKET_CANONICAL_NAMES = Set.of(
			"Java", "JavaScript", "TypeScript", "Node.js", "PostgreSQL", "Vue.js", "React",
			"Spring Boot", "REST APIs", "Docker", "Kubernetes", "C#", ".NET", "MySQL", "SQL",
			"AWS", "Azure", "GCP", "Git", "GitHub", "GitLab", "JUnit", "Mockito", "Testing",
			"CI/CD", "GitHub Actions", "Jenkins", "Linux", "Redis", "MongoDB", "Kafka",
			"RabbitMQ", "Microservices", "Maven", "Gradle", "Hibernate", "JPA", "Angular",
			"Python", "Django", "FastAPI", "Flask", "Express", "NestJS", "Next.js", "HTML",
			"CSS", "Sass", "Tailwind CSS"
	);
	private static final Map<String, ProfessionalKnowledgeEntry> ENTRIES_BY_CANONICAL_KEY =
			buildEntriesByCanonicalKey(ENTRIES);
	private static final Map<String, ProfessionalKnowledgeEntry> ENTRIES_BY_ALIAS_KEY = buildEntriesByAliasKey(ENTRIES);
	private static final Map<String, List<ProfessionalKnowledgeEntry>> ENTRIES_BY_DOMAIN = buildEntriesByDomain(ENTRIES);
	private static final List<ProfessionalKnowledgeEntry> TECHNICAL_MARKET_ENTRIES = ENTRIES.stream()
			.filter(entry -> TECHNICAL_MARKET_CANONICAL_NAMES.contains(entry.canonicalName()))
			.toList();

	private ProfessionalKnowledgeCatalog() {
	}

	public static List<ProfessionalKnowledgeEntry> allEntries() {
		return ENTRIES;
	}

	public static List<ProfessionalKnowledgeEntry> entriesByDomain(ProfessionalDomain domain) {
		return ENTRIES_BY_DOMAIN.getOrDefault(domain.name(), List.of());
	}

	public static List<ProfessionalKnowledgeEntry> technicalMarketEntries() {
		return TECHNICAL_MARKET_ENTRIES;
	}

	public static List<String> technicalMarketCanonicalNames() {
		return TECHNICAL_MARKET_ENTRIES.stream()
				.map(ProfessionalKnowledgeEntry::canonicalName)
				.toList();
	}

	public static Optional<ProfessionalKnowledgeEntry> findByCanonicalName(String canonicalName) {
		if (canonicalName == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(ENTRIES_BY_CANONICAL_KEY.get(comparisonKey(canonicalName)));
	}

	public static Optional<ProfessionalKnowledgeEntry> findByAlias(String alias) {
		if (alias == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(ENTRIES_BY_ALIAS_KEY.get(comparisonKey(alias)));
	}

	public static String canonicalizeProfessionalKnowledge(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		return findByAlias(trimmed)
				.map(ProfessionalKnowledgeEntry::canonicalName)
				.orElse(trimmed);
	}

	public static List<String> normalizeProfessionalKnowledgeList(List<String> values) {
		List<String> normalized = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		for (String value : values) {
			String canonical = canonicalizeProfessionalKnowledge(value);
			if (canonical != null && seen.add(comparisonKey(canonical))) {
				normalized.add(canonical);
			}
		}
		return List.copyOf(normalized);
	}

	static String comparisonKey(String raw) {
		String withoutAccents = Normalizer.normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
		String normalized = DIACRITICS_PATTERN.matcher(withoutAccents).replaceAll("");
		return COMPARISON_SEPARATOR_PATTERN.matcher(normalized).replaceAll("");
	}

	static boolean matchesAlias(String text, ProfessionalKnowledgeEntry entry, String alias) {
		String normalized = normalizeDetectionText(text == null ? "" : text);
		String aliasPattern = normalizeDetectionText(alias).chars()
				.mapToObj(character -> Character.isLetterOrDigit(character)
						? Pattern.quote(String.valueOf((char) character))
						: "[^a-z0-9]+")
				.reduce("", String::concat);
		if ("React".equals(entry.canonicalName())) {
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

	private static List<ProfessionalKnowledgeEntry> buildEntries() {
		List<ProfessionalKnowledgeEntry> entries = new ArrayList<>();

		register(entries, "Java", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "java");
		register(entries, "JavaScript", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "javascript", "js");
		register(entries, "TypeScript", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "typescript", "ts");
		register(entries, "Node.js", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "nodejs", "node.js", "node js");
		register(entries, "PostgreSQL", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.DATA_ANALYTICS), "postgresql", "postgres");
		register(entries, "Vue.js", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "vue", "vuejs", "vue.js");
		register(entries, "React", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "react", "reactjs", "react.js");
		register(entries, "Spring Boot", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "spring boot", "springboot", "spring-boot");
		register(entries, "REST APIs", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "rest api", "rest apis", "api rest", "apis rest", "restful api", "restful apis", "restful service", "restful services");
		register(entries, "Docker", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.OPERATIONS), "docker");
		register(entries, "Kubernetes", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.OPERATIONS), "kubernetes", "k8s");
		register(entries, "C#", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "c#", "csharp");
		register(entries, ".NET", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), ".net", "dotnet");
		register(entries, "MySQL", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.DATA_ANALYTICS), "mysql");
		register(entries, "SQL", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.DATA_ANALYTICS, ProfessionalDomain.ACCOUNTING_FINANCE), "sql");
		register(entries, "AWS", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.OPERATIONS), "aws", "amazon web services");
		register(entries, "Azure", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.OPERATIONS), "azure", "microsoft azure");
		register(entries, "GCP", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.OPERATIONS), "gcp", "google cloud", "google cloud platform");
		register(entries, "Git", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "git");
		register(entries, "GitHub", KnowledgeType.TOOL, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "github");
		register(entries, "GitLab", KnowledgeType.TOOL, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "gitlab");
		register(entries, "JUnit", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "junit");
		register(entries, "Mockito", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "mockito");
		register(entries, "Testing", KnowledgeType.METHODOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.DATA_ANALYTICS), "testing", "tests", "test automation", "automated testing");
		register(entries, "CI/CD", KnowledgeType.METHODOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.OPERATIONS), "ci/cd", "ci cd", "continuous integration", "continuous delivery");
		register(entries, "GitHub Actions", KnowledgeType.TOOL, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.OPERATIONS), "github actions");
		register(entries, "Jenkins", KnowledgeType.TOOL, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.OPERATIONS), "jenkins");
		register(entries, "Linux", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.OPERATIONS), "linux");
		register(entries, "Redis", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "redis");
		register(entries, "MongoDB", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.DATA_ANALYTICS), "mongodb", "mongo db");
		register(entries, "Kafka", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.DATA_ANALYTICS), "kafka", "apache kafka");
		register(entries, "RabbitMQ", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "rabbitmq", "rabbit mq");
		register(entries, "Microservices", KnowledgeType.METHODOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "microservices", "microservice", "micro-services");
		register(entries, "Maven", KnowledgeType.TOOL, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "maven");
		register(entries, "Gradle", KnowledgeType.TOOL, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "gradle");
		register(entries, "Hibernate", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "hibernate");
		register(entries, "JPA", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "jpa", "java persistence api");
		register(entries, "Angular", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "angular");
		register(entries, "Python", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT, ProfessionalDomain.DATA_ANALYTICS), "python");
		register(entries, "Django", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "django");
		register(entries, "FastAPI", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "fastapi", "fast api");
		register(entries, "Flask", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "flask");
		register(entries, "Express", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "express", "express.js", "expressjs");
		register(entries, "NestJS", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "nestjs", "nest js", "nest.js");
		register(entries, "Next.js", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "nextjs", "next js", "next.js");
		register(entries, "HTML", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "html");
		register(entries, "CSS", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "css");
		register(entries, "Sass", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "sass", "scss");
		register(entries, "Tailwind CSS", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.SOFTWARE_DEVELOPMENT), "tailwind css", "tailwind");

		register(entries, "Microsoft Excel", KnowledgeType.TOOL, domains(ProfessionalDomain.ACCOUNTING_FINANCE, ProfessionalDomain.ADMINISTRATION, ProfessionalDomain.DATA_ANALYTICS, ProfessionalDomain.SALES, ProfessionalDomain.HUMAN_RESOURCES, ProfessionalDomain.GENERAL), "microsoft excel", "ms excel", "excel avanzado", "advanced excel", "manejo de excel", "dominio de excel", "planillas excel", "spreadsheets in excel");
		register(entries, "Google Sheets", KnowledgeType.TOOL, domains(ProfessionalDomain.ACCOUNTING_FINANCE, ProfessionalDomain.ADMINISTRATION, ProfessionalDomain.DATA_ANALYTICS, ProfessionalDomain.SALES, ProfessionalDomain.HUMAN_RESOURCES, ProfessionalDomain.GENERAL), "google sheets", "sheets", "hojas de calculo de google");
		register(entries, "Microsoft Word", KnowledgeType.TOOL, domains(ProfessionalDomain.ADMINISTRATION, ProfessionalDomain.HUMAN_RESOURCES, ProfessionalDomain.GENERAL), "microsoft word", "ms word");
		register(entries, "Microsoft PowerPoint", KnowledgeType.TOOL, domains(ProfessionalDomain.ADMINISTRATION, ProfessionalDomain.SALES, ProfessionalDomain.HUMAN_RESOURCES, ProfessionalDomain.GENERAL), "microsoft powerpoint", "ms powerpoint", "powerpoint presentations");
		register(entries, "Microsoft Outlook", KnowledgeType.TOOL, domains(ProfessionalDomain.ADMINISTRATION, ProfessionalDomain.CUSTOMER_SERVICE, ProfessionalDomain.SALES, ProfessionalDomain.GENERAL), "microsoft outlook", "ms outlook");
		register(entries, "Google Workspace", KnowledgeType.TOOL, domains(ProfessionalDomain.ADMINISTRATION, ProfessionalDomain.SALES, ProfessionalDomain.HUMAN_RESOURCES, ProfessionalDomain.GENERAL), "google workspace", "g suite", "google suite");
		register(entries, "Microsoft Teams", KnowledgeType.TOOL, domains(ProfessionalDomain.ADMINISTRATION, ProfessionalDomain.CUSTOMER_SERVICE, ProfessionalDomain.SALES, ProfessionalDomain.HUMAN_RESOURCES, ProfessionalDomain.GENERAL), "microsoft teams", "ms teams");

		register(entries, "Accounts Payable", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.ACCOUNTING_FINANCE), "accounts payable", "cuentas por pagar", "cuentas a pagar");
		register(entries, "Accounts Receivable", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.ACCOUNTING_FINANCE), "accounts receivable", "cuentas por cobrar", "cuentas a cobrar");
		register(entries, "Bank Reconciliation", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.ACCOUNTING_FINANCE), "bank reconciliation", "bank reconciliations", "conciliacion bancaria", "conciliaciones bancarias");
		register(entries, "Invoicing", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.ACCOUNTING_FINANCE, ProfessionalDomain.ADMINISTRATION, ProfessionalDomain.SALES), "invoicing", "facturacion", "emision de facturas", "emision de factura");
		register(entries, "Bookkeeping", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.ACCOUNTING_FINANCE), "bookkeeping", "registracion contable", "registro contable", "teneduria de libros");
		register(entries, "Payroll", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.ACCOUNTING_FINANCE, ProfessionalDomain.HUMAN_RESOURCES), "payroll", "liquidacion de sueldos", "nomina", "gestion de nomina");
		register(entries, "Financial Reporting", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.ACCOUNTING_FINANCE), "financial reporting", "reportes financieros", "informes financieros");
		register(entries, "General Ledger", KnowledgeType.DOMAIN_KNOWLEDGE, domains(ProfessionalDomain.ACCOUNTING_FINANCE), "general ledger", "libro mayor");
		register(entries, "Cost Accounting", KnowledgeType.DOMAIN_KNOWLEDGE, domains(ProfessionalDomain.ACCOUNTING_FINANCE), "cost accounting", "contabilidad de costos");
		register(entries, "SAP", KnowledgeType.TOOL, domains(ProfessionalDomain.ACCOUNTING_FINANCE, ProfessionalDomain.ADMINISTRATION, ProfessionalDomain.OPERATIONS, ProfessionalDomain.HUMAN_RESOURCES), "sap", "sap erp");
		register(entries, "QuickBooks", KnowledgeType.TOOL, domains(ProfessionalDomain.ACCOUNTING_FINANCE), "quickbooks", "quick books");
		register(entries, "Xero", KnowledgeType.TOOL, domains(ProfessionalDomain.ACCOUNTING_FINANCE), "xero");

		register(entries, "Data Entry", KnowledgeType.PROFESSIONAL_SKILL, domains(ProfessionalDomain.ADMINISTRATION, ProfessionalDomain.CUSTOMER_SERVICE, ProfessionalDomain.OPERATIONS), "data entry", "carga de datos");
		register(entries, "Document Management", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.ADMINISTRATION, ProfessionalDomain.HUMAN_RESOURCES, ProfessionalDomain.OPERATIONS), "document management", "gestion documental", "administracion documental");
		register(entries, "Administrative Support", KnowledgeType.PROFESSIONAL_SKILL, domains(ProfessionalDomain.ADMINISTRATION), "administrative support", "tareas administrativas", "soporte administrativo", "asistencia administrativa");
		register(entries, "Scheduling", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.ADMINISTRATION, ProfessionalDomain.HUMAN_RESOURCES, ProfessionalDomain.OPERATIONS), "scheduling", "agenda", "gestion de agenda", "coordinacion de agenda");
		register(entries, "Procurement", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.ADMINISTRATION, ProfessionalDomain.OPERATIONS), "procurement", "compras", "abastecimiento");
		register(entries, "Inventory Management", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.ADMINISTRATION, ProfessionalDomain.OPERATIONS, ProfessionalDomain.SALES), "inventory management", "gestion de inventario", "control de stock", "stock control");
		register(entries, "Purchase Orders", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.ADMINISTRATION, ProfessionalDomain.OPERATIONS, ProfessionalDomain.ACCOUNTING_FINANCE), "purchase orders", "purchase order", "ordenes de compra", "orden de compra");
		register(entries, "Office Management", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.ADMINISTRATION), "office management", "gestion de oficina", "administracion de oficina");

		register(entries, "Customer Service", KnowledgeType.PROFESSIONAL_SKILL, domains(ProfessionalDomain.CUSTOMER_SERVICE, ProfessionalDomain.SALES), "customer service", "atencion al cliente", "atencion al publico", "servicio al cliente");
		register(entries, "Call Center", KnowledgeType.PROFESSIONAL_SKILL, domains(ProfessionalDomain.CUSTOMER_SERVICE, ProfessionalDomain.SALES), "call center", "contact center", "centro de llamadas");
		register(entries, "CRM", KnowledgeType.TOOL, domains(ProfessionalDomain.CUSTOMER_SERVICE, ProfessionalDomain.SALES), "crm", "customer relationship management");
		register(entries, "Ticketing", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.CUSTOMER_SERVICE, ProfessionalDomain.OPERATIONS), "ticketing", "sistema de tickets", "gestion de tickets");
		register(entries, "Complaint Handling", KnowledgeType.PROFESSIONAL_SKILL, domains(ProfessionalDomain.CUSTOMER_SERVICE), "complaint handling", "gestion de reclamos", "manejo de reclamos", "resolucion de reclamos");
		register(entries, "Cash Handling", KnowledgeType.PROFESSIONAL_SKILL, domains(ProfessionalDomain.CUSTOMER_SERVICE, ProfessionalDomain.SALES, ProfessionalDomain.ACCOUNTING_FINANCE), "cash handling", "manejo de caja", "arqueo de caja");
		register(entries, "Point of Sale", KnowledgeType.TOOL, domains(ProfessionalDomain.CUSTOMER_SERVICE, ProfessionalDomain.SALES), "point of sale", "pos", "punto de venta");
		register(entries, "Salesforce", KnowledgeType.TOOL, domains(ProfessionalDomain.CUSTOMER_SERVICE, ProfessionalDomain.SALES), "salesforce");
		register(entries, "Zendesk", KnowledgeType.TOOL, domains(ProfessionalDomain.CUSTOMER_SERVICE), "zendesk");

		register(entries, "Sales", KnowledgeType.PROFESSIONAL_SKILL, domains(ProfessionalDomain.SALES), "sales", "ventas");
		register(entries, "B2B Sales", KnowledgeType.PROFESSIONAL_SKILL, domains(ProfessionalDomain.SALES), "b2b sales", "ventas b2b");
		register(entries, "B2C Sales", KnowledgeType.PROFESSIONAL_SKILL, domains(ProfessionalDomain.SALES), "b2c sales", "ventas b2c");
		register(entries, "Lead Generation", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.SALES), "lead generation", "generacion de leads", "generacion de prospectos");
		register(entries, "Prospecting", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.SALES), "prospecting", "prospeccion", "prospeccion comercial");
		register(entries, "Account Management", KnowledgeType.BUSINESS_PROCESS, domains(ProfessionalDomain.SALES, ProfessionalDomain.CUSTOMER_SERVICE), "account management", "gestion de cuentas", "manejo de cuentas");

		register(entries, "Power BI", KnowledgeType.TOOL, domains(ProfessionalDomain.DATA_ANALYTICS, ProfessionalDomain.ACCOUNTING_FINANCE, ProfessionalDomain.SALES, ProfessionalDomain.OPERATIONS), "power bi", "powerbi");
		register(entries, "Tableau", KnowledgeType.TOOL, domains(ProfessionalDomain.DATA_ANALYTICS, ProfessionalDomain.ACCOUNTING_FINANCE, ProfessionalDomain.SALES), "tableau");
		register(entries, "Pandas", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.DATA_ANALYTICS, ProfessionalDomain.SOFTWARE_DEVELOPMENT), "pandas");
		register(entries, "NumPy", KnowledgeType.TECHNOLOGY, domains(ProfessionalDomain.DATA_ANALYTICS, ProfessionalDomain.SOFTWARE_DEVELOPMENT), "numpy", "num py");

		return List.copyOf(entries);
	}

	private static Set<ProfessionalDomain> domains(ProfessionalDomain first, ProfessionalDomain... rest) {
		Set<ProfessionalDomain> domains = EnumSet.of(first, rest);
		return Collections.unmodifiableSet(domains);
	}

	private static void register(
			List<ProfessionalKnowledgeEntry> entries,
			String canonicalName,
			KnowledgeType type,
			Set<ProfessionalDomain> domains,
			String... aliases
	) {
		List<String> aliasList = new ArrayList<>();
		aliasList.add(canonicalName);
		aliasList.addAll(List.of(aliases));
		entries.add(new ProfessionalKnowledgeEntry(canonicalName, aliasList, domains, type));
	}

	private static Map<String, ProfessionalKnowledgeEntry> buildEntriesByCanonicalKey(List<ProfessionalKnowledgeEntry> entries) {
		Map<String, ProfessionalKnowledgeEntry> byCanonical = new LinkedHashMap<>();
		for (ProfessionalKnowledgeEntry entry : entries) {
			ProfessionalKnowledgeEntry previous = byCanonical.putIfAbsent(comparisonKey(entry.canonicalName()), entry);
			if (previous != null) {
				throw new IllegalStateException("Duplicate canonical knowledge entry: " + entry.canonicalName());
			}
		}
		return Collections.unmodifiableMap(byCanonical);
	}

	private static Map<String, ProfessionalKnowledgeEntry> buildEntriesByAliasKey(List<ProfessionalKnowledgeEntry> entries) {
		Map<String, ProfessionalKnowledgeEntry> byAlias = new LinkedHashMap<>();
		for (ProfessionalKnowledgeEntry entry : entries) {
			for (String alias : entry.aliases()) {
				ProfessionalKnowledgeEntry previous = byAlias.putIfAbsent(comparisonKey(alias), entry);
				if (previous != null && !previous.canonicalName().equals(entry.canonicalName())) {
					throw new IllegalStateException("Alias collision between "
							+ previous.canonicalName() + " and " + entry.canonicalName() + ": " + alias);
				}
			}
		}
		return Collections.unmodifiableMap(byAlias);
	}

	private static Map<String, List<ProfessionalKnowledgeEntry>> buildEntriesByDomain(List<ProfessionalKnowledgeEntry> entries) {
		Map<String, List<ProfessionalKnowledgeEntry>> mutable = new LinkedHashMap<>();
		for (ProfessionalDomain domain : ProfessionalDomain.values()) {
			mutable.put(domain.name(), entries.stream()
					.filter(entry -> entry.domains().contains(domain))
					.toList());
		}
		return Collections.unmodifiableMap(mutable);
	}
}
