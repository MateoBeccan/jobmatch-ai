package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.dto.AnalysisResponse;
import com.codercup.jobmatchai.dto.CriticalRequirementGapResponse;
import com.codercup.jobmatchai.dto.ExperienceGapResponse;
import com.codercup.jobmatchai.dto.JobSearchProfileResponse;
import com.codercup.jobmatchai.dto.RequirementResponse;
import com.codercup.jobmatchai.dto.ScoreBreakdownResponse;
import com.codercup.jobmatchai.dto.internal.GeminiAnalysisResult;
import com.codercup.jobmatchai.exception.InvalidAnalysisRequestException;
import com.codercup.jobmatchai.scoring.MatchScoreCalculator;
import com.codercup.jobmatchai.scoring.MatchScoreResult;
import com.codercup.jobmatchai.scoring.RequirementAssessment;
import com.codercup.jobmatchai.scoring.RequirementCategory;
import com.codercup.jobmatchai.scoring.RequirementCriticality;
import com.codercup.jobmatchai.scoring.RequirementStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AnalysisService {

	private static final long MAX_JOB_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
	private static final int MAX_JOB_IMAGE_DIMENSION = 8000;
	private static final Map<String, Set<String>> ALLOWED_JOB_IMAGE_EXTENSIONS_BY_CONTENT_TYPE = Map.of(
			"image/png", Set.of(".png"),
			"image/jpeg", Set.of(".jpg", ".jpeg"),
			"image/webp", Set.of(".webp")
	);

	private final PdfService pdfService;
	private final CvContentValidator cvContentValidator;
	private final GeminiService geminiService;
	private final MatchScoreCalculator matchScoreCalculator;
	private final ProfessionalKnowledgeExtractor professionalKnowledgeExtractor;
	private final AnalysisEvidenceValidator analysisEvidenceValidator;
	private final int maxJobDescriptionLength;

	public AnalysisService(PdfService pdfService, GeminiService geminiService, MatchScoreCalculator matchScoreCalculator) {
		this(
				pdfService,
				new CvContentValidator(),
				geminiService,
				matchScoreCalculator,
				new ProfessionalKnowledgeExtractor(),
				new AnalysisEvidenceValidator(),
				5000
		);
	}

	@org.springframework.beans.factory.annotation.Autowired
	public AnalysisService(
			PdfService pdfService,
			CvContentValidator cvContentValidator,
			GeminiService geminiService,
			MatchScoreCalculator matchScoreCalculator,
			ProfessionalKnowledgeExtractor professionalKnowledgeExtractor,
			AnalysisEvidenceValidator analysisEvidenceValidator,
			@org.springframework.beans.factory.annotation.Value("${analysis.max-description-length:5000}") int maxJobDescriptionLength
	) {
		this.pdfService = pdfService;
		this.cvContentValidator = cvContentValidator;
		this.geminiService = geminiService;
		this.matchScoreCalculator = matchScoreCalculator;
		this.professionalKnowledgeExtractor = professionalKnowledgeExtractor;
		this.analysisEvidenceValidator = analysisEvidenceValidator;
		this.maxJobDescriptionLength = maxJobDescriptionLength;
	}

	AnalysisService(
			PdfService pdfService,
			CvContentValidator cvContentValidator,
			GeminiService geminiService,
			MatchScoreCalculator matchScoreCalculator,
			int maxJobDescriptionLength
	) {
		this(
				pdfService,
				cvContentValidator,
				geminiService,
				matchScoreCalculator,
				new ProfessionalKnowledgeExtractor(),
				new AnalysisEvidenceValidator(),
				maxJobDescriptionLength
		);
	}

	public AnalysisResponse analyze(MultipartFile cvFile, String jobDescription, MultipartFile jobImage) {
		validateRequest(cvFile, jobDescription, jobImage);
		String cvText = pdfService.extractText(cvFile);
		cvContentValidator.validate(cvText);
		List<ProfessionalKnowledgeEntry> cvKnowledge = professionalKnowledgeExtractor.extract(cvText);
		List<ProfessionalKnowledgeEntry> jobKnowledge = hasText(jobDescription)
				? professionalKnowledgeExtractor.extract(jobDescription)
				: List.of();

		GeminiAnalysisResult aiResult = hasText(jobDescription)
				? geminiService.analyze(
						cvText,
						jobDescription,
						canonicalNames(cvKnowledge),
						canonicalNames(jobKnowledge)
				)
				: geminiService.analyze(cvText, jobImage, canonicalNames(cvKnowledge));

		GeminiAnalysisResult validatedResult = analysisEvidenceValidator.validate(
				aiResult,
				cvKnowledge,
				jobKnowledge,
				hasText(jobDescription)
		);
		return buildAnalysisResponse(validatedResult);
	}

	public AnalysisResponse analyze(MultipartFile cvFile, String jobDescription) {
		return analyze(cvFile, jobDescription, null);
	}

	private void validateRequest(MultipartFile cvFile, String jobDescription, MultipartFile jobImage) {
		validateCvFile(cvFile);

		boolean hasJobDescription = hasText(jobDescription);
		boolean hasJobImage = jobImage != null;

		if (hasJobImage && jobImage.isEmpty()) {
			throw new InvalidAnalysisRequestException("La imagen de la oferta esta vacia.");
		}

		if (!hasJobDescription && !hasJobImage) {
			throw new InvalidAnalysisRequestException("Debes proporcionar la oferta laboral como texto o imagen.");
		}

		if (hasJobDescription && hasJobImage) {
			throw new InvalidAnalysisRequestException("Proporciona la oferta laboral como texto o imagen, no ambas.");
		}

		if (hasJobDescription && jobDescription.length() > maxJobDescriptionLength) {
			throw new InvalidAnalysisRequestException("La descripcion de la oferta no puede superar los "
					+ maxJobDescriptionLength + " caracteres.");
		}

		if (hasJobImage) {
			validateJobImage(jobImage);
		}
	}

	private void validateCvFile(MultipartFile cvFile) {
		if (cvFile == null || cvFile.isEmpty()) {
			throw new InvalidAnalysisRequestException("El archivo del CV no puede estar vacio.");
		}
	}

	private void validateJobImage(MultipartFile jobImage) {
		if (jobImage.getSize() > MAX_JOB_IMAGE_SIZE_BYTES) {
			throw new InvalidAnalysisRequestException("La imagen de la oferta no puede superar los 5 MB.");
		}

		String contentType = jobImage.getContentType();
		String filename = jobImage.getOriginalFilename();
		boolean hasAllowedContentType = contentType != null
				&& ALLOWED_JOB_IMAGE_EXTENSIONS_BY_CONTENT_TYPE.containsKey(contentType.toLowerCase(Locale.ROOT));
		boolean hasAllowedExtension = hasAllowedContentType
				&& filename != null
				&& ALLOWED_JOB_IMAGE_EXTENSIONS_BY_CONTENT_TYPE.get(contentType.toLowerCase(Locale.ROOT))
						.stream()
						.anyMatch(extension -> filename.toLowerCase(Locale.ROOT).endsWith(extension));

		if (!hasAllowedContentType || !hasAllowedExtension) {
			throw new InvalidAnalysisRequestException("La imagen de la oferta debe ser PNG, JPEG o WEBP.");
		}

		try {
			BufferedImage image = ImageIO.read(jobImage.getInputStream());
			if (image == null || image.getWidth() > MAX_JOB_IMAGE_DIMENSION || image.getHeight() > MAX_JOB_IMAGE_DIMENSION) {
				throw new InvalidAnalysisRequestException("La imagen de la oferta no es válida o supera sus dimensiones máximas.");
			}
		} catch (IOException exception) {
			throw new InvalidAnalysisRequestException("No se pudo leer la imagen de la oferta.");
		}
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private List<String> canonicalNames(List<ProfessionalKnowledgeEntry> entries) {
		return entries.stream()
				.map(ProfessionalKnowledgeEntry::canonicalName)
				.toList();
	}

	private AnalysisResponse buildAnalysisResponse(GeminiAnalysisResult aiResult) {
		MatchScoreResult score = matchScoreCalculator.calculate(aiResult.requirements());
		List<CriticalRequirementGapResponse> criticalMissingRequirements =
				extractCriticalMissingRequirements(aiResult.requirements());
		ExperienceGapResponse experienceGap = determineExperienceGap(aiResult.requirements());
		return new AnalysisResponse(
				score.matchPercentage(),
				aiResult.matchingSkills(),
				aiResult.missingSkills(),
				criticalMissingRequirements,
				experienceGap,
				buildWarnings(score, criticalMissingRequirements, experienceGap),
				aiResult.recommendations(),
				aiResult.interviewQuestions(),
				aiResult.requirements().stream()
						.map(requirement -> new RequirementResponse(
								requirement.name(),
								formatCategory(requirement.category()),
								requirement.status().name().toLowerCase(Locale.ROOT),
								requirement.evidence()
						))
						.toList(),
				new ScoreBreakdownResponse(
						score.breakdown().mandatoryTechnical(),
						score.breakdown().experienceSeniority(),
						score.breakdown().desirable(),
						score.breakdown().complementary()
				),
				new JobSearchProfileResponse(
						aiResult.jobSearchProfile().role(),
						aiResult.jobSearchProfile().seniority(),
						aiResult.jobSearchProfile().keywords()
				)
		);
	}

	private List<CriticalRequirementGapResponse> extractCriticalMissingRequirements(
			List<RequirementAssessment> requirements
	) {
		return requirements.stream()
				.filter(requirement -> requirement.criticality() == RequirementCriticality.CRITICAL)
				.filter(requirement -> requirement.status() == RequirementStatus.MISSING)
				.map(requirement -> new CriticalRequirementGapResponse(
						requirement.name(),
						formatCategory(requirement.category()),
						requirement.evidence()
				))
				.toList();
	}

	private ExperienceGapResponse determineExperienceGap(List<RequirementAssessment> requirements) {
		Optional<RequirementAssessment> experienceRequirement = requirements.stream()
				.filter(requirement -> requirement.category() == RequirementCategory.EXPERIENCE_SENIORITY)
				.filter(requirement -> requirement.status() == RequirementStatus.MISSING
						|| requirement.status() == RequirementStatus.PARTIAL)
				.min(Comparator.comparingInt(this::experienceGapPriority));

		return experienceRequirement
				.map(requirement -> new ExperienceGapResponse(
						requirement.name(),
						requirement.status().name().toLowerCase(Locale.ROOT),
						requirement.criticality() == RequirementCriticality.CRITICAL,
						buildExperienceGapSummary(requirement)
				))
				.orElse(null);
	}

	private int experienceGapPriority(RequirementAssessment requirement) {
		if (requirement.criticality() == RequirementCriticality.CRITICAL
				&& requirement.status() == RequirementStatus.MISSING) {
			return 0;
		}
		if (requirement.criticality() == RequirementCriticality.CRITICAL
				&& requirement.status() == RequirementStatus.PARTIAL) {
			return 1;
		}
		if (requirement.status() == RequirementStatus.MISSING) {
			return 2;
		}
		return 3;
	}

	private String buildExperienceGapSummary(RequirementAssessment requirement) {
		if (requirement.evidence() != null && !requirement.evidence().isBlank()) {
			return requirement.evidence();
		}
		if (requirement.status() == RequirementStatus.PARTIAL) {
			return "La experiencia requerida esta parcialmente respaldada por el CV.";
		}
		return "La experiencia requerida no esta respaldada por el CV.";
	}

	private List<String> buildWarnings(
			MatchScoreResult score,
			List<CriticalRequirementGapResponse> criticalMissingRequirements,
			ExperienceGapResponse experienceGap
	) {
		List<String> warnings = new ArrayList<>();
		int criticalMissingCount = criticalMissingRequirements.size();
		if (criticalMissingCount == 1) {
			warnings.add("Falta 1 requisito critico de la oferta.");
		}
		else if (criticalMissingCount > 1) {
			warnings.add("Faltan " + criticalMissingCount + " requisitos criticos de la oferta.");
		}
		if (score.criticalPartialCount() == 1) {
			warnings.add("Un requisito critico se cumple parcialmente.");
		}
		else if (score.criticalPartialCount() > 1) {
			warnings.add("Varios requisitos criticos se cumplen parcialmente.");
		}
		if (experienceGap != null) {
			warnings.add("La experiencia profesional requerida no esta completamente respaldada por el CV.");
		}
		if (score.criticalCapApplied()) {
			if (score.criticalMissingCount() > 0) {
				warnings.add("El score esta limitado por requisitos criticos no cumplidos.");
			}
			else {
				warnings.add("El score esta limitado por un requisito critico parcialmente cumplido.");
			}
		}
		return List.copyOf(warnings);
	}

	private String formatCategory(RequirementCategory category) {
		return category.name().toLowerCase(Locale.ROOT);
	}

}
