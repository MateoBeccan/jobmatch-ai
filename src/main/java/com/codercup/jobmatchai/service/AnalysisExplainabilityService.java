package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.dto.RequirementExplainabilityResponse;
import com.codercup.jobmatchai.dto.ScoreExplanationResponse;
import com.codercup.jobmatchai.dto.internal.GeminiAnalysisResult;
import com.codercup.jobmatchai.scoring.MatchScoreResult;
import com.codercup.jobmatchai.scoring.RequirementAssessment;
import com.codercup.jobmatchai.scoring.RequirementCategory;
import com.codercup.jobmatchai.scoring.RequirementStatus;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AnalysisExplainabilityService {

	private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}");
	private static final Pattern OR_PATTERN = Pattern.compile("(^|\\s)(or|o)(\\s|$)");
	private static final Pattern AND_PATTERN = Pattern.compile("(^|\\s)(and|y)(\\s|$)");
	private static final Pattern YEARS_PATTERN = Pattern.compile("(^|\\s)\\d+\\+?\\s*(years?|anos?|anios?)(\\s|$)");
	private static final Pattern VERSION_PATTERN = Pattern.compile("(^|\\s)[a-z#+.]+\\s*\\d+\\+?(\\s|$)");
	private static final Pattern COMPLEX_LANGUAGE_PATTERN = Pattern.compile(
			"(^|\\s)(equivalent|equivalente|similar|advanced|avanzado|avanzada|expert|experto|experta|"
					+ "intermediate|intermedio|intermedia|basic|basico|basica|mastery)(\\s|$)"
					+ "|(^|\\s)(dominio|manejo)\\s+de(\\s|$)"
	);

	public List<RequirementExplanation> explainRequirements(
			GeminiAnalysisResult originalResult,
			GeminiAnalysisResult validatedResult,
			List<ProfessionalKnowledgeEntry> cvKnowledge,
			List<ProfessionalKnowledgeEntry> jobKnowledge,
			boolean hasTextualJobEvidence
	) {
		List<RequirementAssessment> validatedRequirements = validatedResult.requirements();
		List<RequirementAssessment> originalRequirements = originalResult.requirements();
		Set<String> cvKeys = canonicalKeys(cvKnowledge);
		Set<String> jobKeys = canonicalKeys(jobKnowledge);
		List<RequirementExplanation> explanations = new ArrayList<>();
		for (int index = 0; index < validatedRequirements.size(); index++) {
			RequirementAssessment validated = validatedRequirements.get(index);
			RequirementAssessment original = matchingOriginalRequirement(originalRequirements, index, validated)
					.orElse(null);
			explanations.add(explainRequirement(validated, original, cvKeys, jobKeys, hasTextualJobEvidence));
		}
		return List.copyOf(explanations);
	}

	public ScoreExplanationResponse explainScore(MatchScoreResult score) {
		String capReason = capReason(score);
		return new ScoreExplanationResponse(
				score.basePercentage(),
				score.matchPercentage(),
				score.criticalCapApplied(),
				score.criticalMissingCount(),
				score.criticalPartialCount(),
				capReason,
				scoreSummary(score, capReason)
		);
	}

	private Optional<RequirementAssessment> matchingOriginalRequirement(
			List<RequirementAssessment> originalRequirements,
			int index,
			RequirementAssessment validated
	) {
		if (index >= originalRequirements.size()) {
			return Optional.empty();
		}
		RequirementAssessment original = originalRequirements.get(index);
		if (sameRequirementIdentity(original, validated)) {
			return Optional.of(original);
		}
		return Optional.empty();
	}

	private boolean sameRequirementIdentity(RequirementAssessment left, RequirementAssessment right) {
		return left.name().equals(right.name())
				&& left.category() == right.category()
				&& left.criticality() == right.criticality();
	}

	private RequirementExplanation explainRequirement(
			RequirementAssessment validated,
			RequirementAssessment original,
			Set<String> cvKeys,
			Set<String> jobKeys,
			boolean hasTextualJobEvidence
	) {
		boolean statusAdjusted = original != null && original.status() != validated.status();
		String originalStatus = statusAdjusted ? formatStatus(original.status()) : null;
		Optional<ProfessionalKnowledgeEntry> knownEntry = ProfessionalKnowledgeCatalog.findByCanonicalOrAlias(validated.name());
		if (isDeterministicallyExplainableAtomicRequirement(validated, knownEntry)) {
			String key = canonicalKey(knownEntry.get());
			boolean cvDetected = cvKeys.contains(key);
			Boolean jobDetected = hasTextualJobEvidence ? jobKeys.contains(key) : null;
			String evidence = deterministicEvidence(validated, original, knownEntry.get(), cvDetected, statusAdjusted);
			return new RequirementExplanation(
					validated,
					evidence,
					new RequirementExplainabilityResponse(
							"deterministic_catalog",
							statusAdjusted,
							originalStatus,
							cvDetected,
							jobDetected,
							deterministicSummary(validated, knownEntry.get(), cvDetected, statusAdjusted)
					)
			);
		}

		String evidenceBasis = validated.category() == RequirementCategory.EXPERIENCE_SENIORITY
				? "experience_semantic_analysis"
				: "semantic_analysis";
		String evidence = semanticEvidence(validated, evidenceBasis, knownEntry);
		return new RequirementExplanation(
				validated,
				evidence,
				new RequirementExplainabilityResponse(
						evidenceBasis,
						statusAdjusted,
						originalStatus,
						null,
						null,
						semanticSummary(validated, evidenceBasis, statusAdjusted)
				)
		);
	}

	boolean isDeterministicallyExplainableAtomicRequirement(RequirementAssessment requirement) {
		return isDeterministicallyExplainableAtomicRequirement(
				requirement,
				ProfessionalKnowledgeCatalog.findByCanonicalOrAlias(requirement.name())
		);
	}

	private boolean isDeterministicallyExplainableAtomicRequirement(
			RequirementAssessment requirement,
			Optional<ProfessionalKnowledgeEntry> knownEntry
	) {
		if (requirement.category() == RequirementCategory.EXPERIENCE_SENIORITY) {
			return false;
		}
		return knownEntry.isPresent() && !hasSemanticQualifierOrComposition(requirement.name());
	}

	private boolean hasSemanticQualifierOrComposition(String requirementName) {
		String normalized = normalizeForComplexityCheck(requirementName);
		return OR_PATTERN.matcher(normalized).find()
				|| AND_PATTERN.matcher(normalized).find()
				|| YEARS_PATTERN.matcher(normalized).find()
				|| VERSION_PATTERN.matcher(normalized).find()
				|| COMPLEX_LANGUAGE_PATTERN.matcher(normalized).find();
	}

	private String normalizeForComplexityCheck(String value) {
		String withoutAccents = java.text.Normalizer.normalize(value.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD);
		String normalized = DIACRITICS_PATTERN.matcher(withoutAccents).replaceAll("");
		return normalized.replaceAll("[^a-z0-9#+.]+", " ").trim().replaceAll("\\s+", " ");
	}

	private String deterministicEvidence(
			RequirementAssessment requirement,
			RequirementAssessment original,
			ProfessionalKnowledgeEntry entry,
			boolean cvDetected,
			boolean statusAdjusted
	) {
		if (requirement.status() == RequirementStatus.MISSING) {
			return "No se detecto una mencion de " + entry.canonicalName() + " en el texto extraido del CV.";
		}
		if (requirement.status() == RequirementStatus.PARTIAL) {
			if (statusAdjusted && original != null && original.status() == RequirementStatus.MISSING) {
				return "Se detecto " + entry.canonicalName()
						+ " en el CV. La validacion local corrigio el estado de MISSING a PARTIAL; "
						+ "la mencion textual no demuestra por si sola nivel de dominio o experiencia profesional.";
			}
			return cvDetected
					? "Se detecto " + entry.canonicalName()
							+ " en el CV, pero no quedo suficientemente respaldado como cumplimiento completo."
					: "El requisito no quedo suficientemente respaldado por el CV analizado.";
		}
		return cvDetected
				? "El CV contiene evidencia textual de " + entry.canonicalName()
						+ " y el analisis lo clasifico como MATCH."
				: "El requisito fue clasificado como MATCH por interpretacion semantica del analisis.";
	}

	private String deterministicSummary(
			RequirementAssessment requirement,
			ProfessionalKnowledgeEntry entry,
			boolean cvDetected,
			boolean statusAdjusted
	) {
		if (statusAdjusted) {
			if (requirement.status() == RequirementStatus.MISSING && !cvDetected) {
				return "La validacion local corrigio el estado porque " + entry.canonicalName()
						+ " no fue detectado en el CV.";
			}
			if (requirement.status() == RequirementStatus.PARTIAL && cvDetected) {
				return "La validacion local encontro evidencia textual, pero no la elevo a cumplimiento completo.";
			}
			return "La validacion local ajusto el estado final segun la evidencia textual detectada.";
		}
		if (requirement.status() == RequirementStatus.MATCH) {
			return "El requisito conocido quedo respaldado por evidencia textual local y analisis semantico.";
		}
		if (requirement.status() == RequirementStatus.PARTIAL) {
			return "Existe evidencia textual local, pero el requisito no queda completamente demostrado.";
		}
		return "El catalogo local no detecto evidencia textual suficiente para este requisito conocido.";
	}

	private String semanticEvidence(
			RequirementAssessment requirement,
			String evidenceBasis,
			Optional<ProfessionalKnowledgeEntry> relatedKnownEntry
	) {
		if ("experience_semantic_analysis".equals(evidenceBasis)) {
			if (requirement.status() == RequirementStatus.PARTIAL) {
				return "El CV aporta evidencia relacionada, pero no respalda completamente la experiencia solicitada.";
			}
			if (requirement.status() == RequirementStatus.MISSING) {
				return "El requisito de experiencia no quedo suficientemente respaldado por la informacion del CV analizada semanticamente.";
			}
			return "El requisito de experiencia fue interpretado semanticamente como respaldado por el CV.";
		}
		if (relatedKnownEntry.isPresent() && requirement.status() == RequirementStatus.PARTIAL) {
			return "El CV aporta evidencia relacionada con " + relatedKnownEntry.get().canonicalName()
					+ ", pero el requisito completo requiere interpretacion semantica y no puede validarse solo por "
					+ "la deteccion del catalogo.";
		}
		if (requirement.status() == RequirementStatus.PARTIAL) {
			return "El requisito fue interpretado semanticamente como parcialmente respaldado por el CV.";
		}
		if (requirement.status() == RequirementStatus.MISSING) {
			return "El requisito no quedo suficientemente respaldado por el CV analizado semanticamente.";
		}
		return "El requisito fue interpretado semanticamente como respaldado por el CV.";
	}

	private String semanticSummary(
			RequirementAssessment requirement,
			String evidenceBasis,
			boolean statusAdjusted
	) {
		String prefix = statusAdjusted
				? "El estado final fue ajustado tras la validacion local. "
				: "";
		if ("experience_semantic_analysis".equals(evidenceBasis)) {
			return prefix + "La experiencia se explica por interpretacion semantica, no por validacion de catalogo.";
		}
		return prefix + "El requisito no corresponde a una knowledge atomica verificable por el catalogo local.";
	}

	private String capReason(MatchScoreResult score) {
		if (!score.criticalCapApplied()) {
			return "none";
		}
		if (score.criticalMissingCount() >= 2) {
			return "multiple_critical_missing";
		}
		if (score.criticalMissingCount() == 1) {
			return "single_critical_missing";
		}
		if (score.criticalPartialCount() > 0) {
			return "critical_partial";
		}
		return "none";
	}

	private String scoreSummary(MatchScoreResult score, String capReason) {
		return switch (capReason) {
			case "critical_partial" -> "El score base fue " + score.basePercentage()
					+ "%, pero el resultado final quedo limitado a " + score.matchPercentage()
					+ "% porque al menos un requisito critico se cumple parcialmente.";
			case "single_critical_missing" -> "El score base fue " + score.basePercentage()
					+ "%, pero el resultado final quedo limitado a " + score.matchPercentage()
					+ "% porque falta un requisito critico.";
			case "multiple_critical_missing" -> "El score base fue " + score.basePercentage()
					+ "%, pero el resultado final quedo limitado a " + score.matchPercentage()
					+ "% porque faltan varios requisitos criticos.";
			default -> "El porcentaje final coincide con el score base.";
		};
	}

	private String formatStatus(RequirementStatus status) {
		return status.name().toLowerCase(Locale.ROOT);
	}

	private Set<String> canonicalKeys(List<ProfessionalKnowledgeEntry> entries) {
		Set<String> keys = new LinkedHashSet<>();
		for (ProfessionalKnowledgeEntry entry : entries) {
			keys.add(canonicalKey(entry));
		}
		return keys;
	}

	private String canonicalKey(ProfessionalKnowledgeEntry entry) {
		return ProfessionalKnowledgeCatalog.comparisonKey(entry.canonicalName());
	}

	public record RequirementExplanation(
			RequirementAssessment requirement,
			String finalEvidence,
			RequirementExplainabilityResponse explainability
	) {
	}
}
