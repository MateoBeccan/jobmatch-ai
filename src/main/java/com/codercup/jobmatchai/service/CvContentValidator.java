package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.exception.InvalidCvContentException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class CvContentValidator {

	private static final int MIN_TEXT_LENGTH = 120;
	private static final int MIN_WORD_COUNT = 18;
	private static final int MIN_POSITIVE_CATEGORIES = 3;
	private static final int MIN_STRUCTURAL_CATEGORIES_WITH_COMMERCIAL_SIGNALS = 2;
	private static final int MANY_COMMERCIAL_SIGNALS = 3;
	private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[\\w.%+-]+@[\\w.-]+\\.[a-z]{2,}\\b");
	private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?\\d[\\d\\s().-]{7,}\\d)");
	private static final Set<Category> STRUCTURAL_CATEGORIES = Set.of(
			Category.PROFILE,
			Category.EXPERIENCE,
			Category.SKILLS,
			Category.PROJECTS
	);

	private static final Map<Category, List<String>> POSITIVE_SIGNALS = Map.of(
			Category.PROFILE, List.of(
					"perfil profesional", "resumen profesional", "sobre mi", "objetivo profesional",
					"professional summary", "profile", "objective", "about me"
			),
			Category.EXPERIENCE, List.of(
					"experiencia", "experiencia laboral", "experiencia profesional", "work experience",
					"employment", "employment history", "work history", "professional experience"
			),
			Category.EDUCATION, List.of(
					"educacion", "formacion", "estudios", "formacion academica", "universidad",
					"instituto", "tecnicatura", "licenciatura", "ingenieria", "education",
					"university", "college", "degree", "academic background"
			),
			Category.SKILLS, List.of(
					"habilidades", "habilidades tecnicas", "tecnologias", "conocimientos",
					"herramientas", "competencias", "skills", "technical skills", "technologies",
					"tools", "competencies"
			),
			Category.PROJECTS, List.of(
					"proyectos", "proyectos personales", "proyectos academicos", "projects",
					"personal projects", "academic projects"
			),
			Category.CERTIFICATIONS, List.of(
					"certificaciones", "certificacion", "cursos", "certificates",
					"certifications", "courses"
			),
			Category.LANGUAGES, List.of(
					"idiomas", "ingles", "espanol", "languages", "english", "spanish"
			)
	);

	private static final List<String> CONTACT_SIGNALS = List.of("linkedin", "github", "portfolio", "portafolio");
	private static final List<String> COMMERCIAL_SIGNALS = List.of(
			"factura", "factura a", "factura b", "factura c", "invoice", "comprobante",
			"cae", "cuit", "iva", "vat", "subtotal", "importe total", "condicion de venta",
			"vencimiento", "punto de venta", "nro comprobante", "invoice number", "total"
	);

	public void validate(String cvText) {
		String normalized = normalize(cvText);
		if (normalized.length() < MIN_TEXT_LENGTH || wordCount(normalized) < MIN_WORD_COUNT) {
			throw new InvalidCvContentException();
		}

		Set<Category> categories = detectedCategories(normalized);
		int commercialSignals = countSignals(normalized, COMMERCIAL_SIGNALS);
		int structuralCategories = structuralCategoryCount(categories);
		boolean enoughCvEvidence = categories.size() >= MIN_POSITIVE_CATEGORIES && structuralCategories >= 1;
		boolean commercialDocumentWithWeakCvEvidence = commercialSignals >= MANY_COMMERCIAL_SIGNALS
				&& structuralCategories < MIN_STRUCTURAL_CATEGORIES_WITH_COMMERCIAL_SIGNALS;

		if (!enoughCvEvidence || commercialDocumentWithWeakCvEvidence) {
			throw new InvalidCvContentException();
		}
	}

	private Set<Category> detectedCategories(String normalized) {
		Set<Category> categories = new java.util.HashSet<>();
		if (EMAIL_PATTERN.matcher(normalized).find()
				|| PHONE_PATTERN.matcher(normalized).find()
				|| containsAny(normalized, CONTACT_SIGNALS)) {
			categories.add(Category.CONTACT);
		}

		POSITIVE_SIGNALS.forEach((category, signals) -> {
			if (containsAny(normalized, signals)) {
				categories.add(category);
			}
		});

		return categories;
	}

	private String normalize(String value) {
		if (value == null) {
			return "";
		}
		String lowerCase = value.toLowerCase(Locale.ROOT).trim();
		String withoutAccents = Normalizer.normalize(lowerCase, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		return withoutAccents.replaceAll("\\s+", " ");
	}

	private int wordCount(String value) {
		if (value.isBlank()) {
			return 0;
		}
		return value.split("\\s+").length;
	}

	private boolean containsAny(String value, List<String> signals) {
		return signals.stream().anyMatch(signal -> containsSignal(value, signal));
	}

	private int countSignals(String value, List<String> signals) {
		int count = 0;
		for (String signal : signals) {
			if (containsSignal(value, signal)) {
				count++;
			}
		}
		return count;
	}

	private boolean containsSignal(String value, String signal) {
		Pattern pattern = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(signal) + "(?![\\p{L}\\p{N}])");
		return pattern.matcher(value).find();
	}

	private int structuralCategoryCount(Set<Category> categories) {
		int count = 0;
		for (Category category : STRUCTURAL_CATEGORIES) {
			if (categories.contains(category)) {
				count++;
			}
		}
		return count;
	}

	private enum Category {
		CONTACT,
		PROFILE,
		EXPERIENCE,
		EDUCATION,
		SKILLS,
		PROJECTS,
		CERTIFICATIONS,
		LANGUAGES
	}
}
