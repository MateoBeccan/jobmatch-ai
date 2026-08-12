package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.dto.AnalysisResponse;
import com.codercup.jobmatchai.exception.InvalidAnalysisRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AnalysisService {

	private final PdfService pdfService;
	private final GeminiService geminiService;

	public AnalysisService(PdfService pdfService, GeminiService geminiService) {
		this.pdfService = pdfService;
		this.geminiService = geminiService;
	}

	public AnalysisResponse analyze(MultipartFile cvFile, String jobDescription) {
		validateRequest(cvFile, jobDescription);
		String cvText = pdfService.extractText(cvFile);

		return geminiService.analyze(cvText, jobDescription);
	}

	private void validateRequest(MultipartFile cvFile, String jobDescription) {
		if (cvFile == null || cvFile.isEmpty()) {
			throw new InvalidAnalysisRequestException("El archivo del CV no puede estar vacio.");
		}

		if (jobDescription == null || jobDescription.isBlank()) {
			throw new InvalidAnalysisRequestException("La descripcion de la oferta laboral no puede estar vacia.");
		}
	}
}
