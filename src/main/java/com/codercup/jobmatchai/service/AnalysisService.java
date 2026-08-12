package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.dto.AnalysisResponse;
import com.codercup.jobmatchai.exception.InvalidAnalysisRequestException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AnalysisService {

	private final PdfService pdfService;

	public AnalysisService(PdfService pdfService) {
		this.pdfService = pdfService;
	}

	public AnalysisResponse analyze(MultipartFile cvFile, String jobDescription) {
		validateRequest(cvFile, jobDescription);
		String cvText = pdfService.extractText(cvFile);

		return new AnalysisResponse(
				0,
				List.of(),
				List.of(),
				List.of(),
				List.of()
		);
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
