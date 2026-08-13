package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.dto.AnalysisResponse;
import com.codercup.jobmatchai.dto.internal.GeminiAnalysisResult;
import com.codercup.jobmatchai.exception.InvalidAnalysisRequestException;
import com.codercup.jobmatchai.scoring.MatchScoreCalculator;
import com.codercup.jobmatchai.scoring.MatchScoreResult;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AnalysisService {

	private static final long MAX_JOB_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
	private static final int MAX_JOB_DESCRIPTION_LENGTH = 15000;
	private static final Map<String, Set<String>> ALLOWED_JOB_IMAGE_EXTENSIONS_BY_CONTENT_TYPE = Map.of(
			"image/png", Set.of(".png"),
			"image/jpeg", Set.of(".jpg", ".jpeg"),
			"image/webp", Set.of(".webp")
	);

	private final PdfService pdfService;
	private final GeminiService geminiService;
	private final MatchScoreCalculator matchScoreCalculator;

	public AnalysisService(
			PdfService pdfService,
			GeminiService geminiService,
			MatchScoreCalculator matchScoreCalculator
	) {
		this.pdfService = pdfService;
		this.geminiService = geminiService;
		this.matchScoreCalculator = matchScoreCalculator;
	}

	public AnalysisResponse analyze(MultipartFile cvFile, String jobDescription, MultipartFile jobImage) {
		validateRequest(cvFile, jobDescription, jobImage);
		String cvText = pdfService.extractText(cvFile);

		GeminiAnalysisResult aiResult = hasText(jobDescription)
				? geminiService.analyze(cvText, jobDescription)
				: geminiService.analyze(cvText, jobImage);

		return buildAnalysisResponse(aiResult);
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

		if (hasJobDescription && jobDescription.length() > MAX_JOB_DESCRIPTION_LENGTH) {
			throw new InvalidAnalysisRequestException("La descripcion de la oferta no puede superar los 15000 caracteres.");
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
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private AnalysisResponse buildAnalysisResponse(GeminiAnalysisResult aiResult) {
		MatchScoreResult score = matchScoreCalculator.calculate(aiResult.requirements());
		return new AnalysisResponse(
				score.matchPercentage(),
				aiResult.matchingSkills(),
				aiResult.missingSkills(),
				aiResult.recommendations(),
				aiResult.interviewQuestions()
		);
	}

}
