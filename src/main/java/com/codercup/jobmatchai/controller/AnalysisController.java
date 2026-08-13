package com.codercup.jobmatchai.controller;

import com.codercup.jobmatchai.dto.AnalysisResponse;
import com.codercup.jobmatchai.dto.AnalysisHistoryResponse;
import com.codercup.jobmatchai.dto.AnalysisHistoryPageResponse;
import com.codercup.jobmatchai.service.AnalysisService;
import com.codercup.jobmatchai.service.AnalysisHistoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AnalysisController {

	private final AnalysisService analysisService;
	private final AnalysisHistoryService analysisHistoryService;

	public AnalysisController(AnalysisService analysisService, AnalysisHistoryService analysisHistoryService) {
		this.analysisService = analysisService;
		this.analysisHistoryService = analysisHistoryService;
	}

	@PostMapping(path = "/api/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public AnalysisResponse analyze(
			@RequestPart(value = "cvFile", required = false) MultipartFile cvFile,
			@RequestPart(value = "jobDescription", required = false) String jobDescription,
			@RequestPart(value = "jobImage", required = false) MultipartFile jobImage
	) {
		return analysisService.analyze(cvFile, jobDescription, jobImage);
	}

	@PostMapping(path = "/api/analyses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public AnalysisHistoryResponse createAnalysis(
			@RequestPart("cvFile") MultipartFile cvFile,
			@RequestPart(value = "jobDescription", required = false) String jobDescription,
			@RequestPart(value = "jobImage", required = false) MultipartFile jobImage,
			@RequestPart(value = "cvVersion", required = false) String cvVersion
	) {
		AnalysisResponse result = analysisService.analyze(cvFile, jobDescription, jobImage);
		String normalizedDescription = jobDescription == null ? "Oferta desde imagen" : jobDescription.trim();
		String[] metadata = extractOfferMetadata(normalizedDescription);
		return analysisHistoryService.save(
				cvFile.getOriginalFilename(),
				cvVersion == null || cvVersion.isBlank() ? "CV sin versión" : cvVersion.trim(),
				metadata[0], metadata[1], normalizedDescription,
				jobImage == null ? "text" : "image", result
		);
	}

	@GetMapping("/api/analyses")
	public AnalysisHistoryPageResponse getAnalyses(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return analysisHistoryService.findPage(page, size);
	}

	@GetMapping("/api/analyses/{id}")
	public AnalysisHistoryResponse getAnalysis(@PathVariable String id) {
		return analysisHistoryService.findById(id);
	}

	@DeleteMapping("/api/analyses/{id}")
	public void deleteAnalysis(@PathVariable String id) {
		analysisHistoryService.deleteById(id);
	}

	private String[] extractOfferMetadata(String description) {
		String[] lines = description.split("\\R");
		String role = lines.length > 0 && !lines[0].isBlank() ? lines[0].trim() : "Nueva oferta";
		return new String[] { role.substring(0, Math.min(role.length(), 160)), "Oferta laboral" };
	}
}
