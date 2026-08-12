package com.codercup.jobmatchai.controller;

import com.codercup.jobmatchai.dto.AnalysisResponse;
import com.codercup.jobmatchai.service.AnalysisService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AnalysisController {

	private final AnalysisService analysisService;

	public AnalysisController(AnalysisService analysisService) {
		this.analysisService = analysisService;
	}

	@PostMapping(path = "/api/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public AnalysisResponse analyze(
			@RequestPart(value = "cvFile", required = false) MultipartFile cvFile,
			@RequestPart(value = "jobDescription", required = false) String jobDescription,
			@RequestPart(value = "jobImage", required = false) MultipartFile jobImage
	) {
		return analysisService.analyze(cvFile, jobDescription, jobImage);
	}
}
