package com.codercup.jobmatchai.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobicySearchResponse(
		List<JobicyJob> jobs
) {
}
