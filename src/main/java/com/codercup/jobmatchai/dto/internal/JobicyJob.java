package com.codercup.jobmatchai.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobicyJob(
		Object id,
		String url,
		String jobTitle,
		String companyName,
		JsonNode jobIndustry,
		JsonNode jobType,
		String jobGeo,
		String jobLevel,
		String jobExcerpt,
		String jobDescription,
		String pubDate,
		Object salaryMin,
		Object salaryMax,
		String salaryCurrency,
		String salaryPeriod
) {
}
