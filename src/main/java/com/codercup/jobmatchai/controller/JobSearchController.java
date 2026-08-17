package com.codercup.jobmatchai.controller;

import com.codercup.jobmatchai.dto.JobSearchRequest;
import com.codercup.jobmatchai.dto.JobSearchResponse;
import com.codercup.jobmatchai.service.JobSearchService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobSearchController {

	private final JobSearchService jobSearchService;

	public JobSearchController(JobSearchService jobSearchService) {
		this.jobSearchService = jobSearchService;
	}

	@PostMapping(path = "/api/jobs/search", consumes = MediaType.APPLICATION_JSON_VALUE)
	public JobSearchResponse search(@RequestBody(required = false) JobSearchRequest request) {
		return jobSearchService.search(request);
	}
}
