package com.codercup.jobmatchai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codercup.jobmatchai.dto.JobOfferResponse;
import com.codercup.jobmatchai.dto.JobSearchRequest;
import com.codercup.jobmatchai.dto.JobSearchResponse;
import com.codercup.jobmatchai.exception.ApiExceptionHandler;
import com.codercup.jobmatchai.exception.InvalidJobSearchRequestException;
import com.codercup.jobmatchai.exception.InvalidJobSearchResponseException;
import com.codercup.jobmatchai.exception.JobSearchConfigurationException;
import com.codercup.jobmatchai.exception.JobSearchTimeoutException;
import com.codercup.jobmatchai.exception.JobSearchUnavailableException;
import com.codercup.jobmatchai.service.JobSearchService;
import java.net.SocketTimeoutException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobSearchController.class)
@Import({ApiExceptionHandler.class, JobSearchControllerTest.JobSearchTestConfiguration.class})
class JobSearchControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JobSearchService jobSearchService;

	@BeforeEach
	void resetMock() {
		reset(jobSearchService);
	}

	@Test
	void searchReturnsOkForValidRequest() throws Exception {
		when(jobSearchService.search(any(JobSearchRequest.class))).thenReturn(new JobSearchResponse(
				"JOBICY",
				1,
				List.of(new JobOfferResponse(
						"123",
						"Java Backend Developer",
						"Example Tech",
						"Rosario",
						"Java con Spring Boot",
						null,
						"Full-time",
						"2026-08-16T15:30:00Z",
						"https://example.com/job/123",
						"Jobicy",
						List.of("Java", "Spring Boot")
				))
		));

		mockMvc.perform(post("/api/jobs/search")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.provider").value("JOBICY"))
				.andExpect(jsonPath("$.count").value(1))
				.andExpect(jsonPath("$.jobs[0].title").value("Java Backend Developer"))
				.andExpect(jsonPath("$.jobs[0].matchedKeywords[0]").value("Java"));
	}

	@Test
	void invalidRequestReturnsBadRequestCode() throws Exception {
		when(jobSearchService.search(any())).thenThrow(new InvalidJobSearchRequestException());

		mockMvc.perform(post("/api/jobs/search")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_JOB_SEARCH_REQUEST"));
	}

	@Test
	void malformedJsonReturnsJobSearchRequestCode() throws Exception {
		mockMvc.perform(post("/api/jobs/search")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{not-json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_JOB_SEARCH_REQUEST"))
				.andExpect(jsonPath("$.message").value("Los criterios de busqueda de ofertas no son validos."));
	}

	@Test
	void unavailableProviderReturnsServiceUnavailableCode() throws Exception {
		when(jobSearchService.search(any())).thenThrow(new JobSearchUnavailableException("unavailable"));

		mockMvc.perform(post("/api/jobs/search")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson()))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("JOB_SEARCH_UNAVAILABLE"));
	}

	@Test
	void timeoutReturnsGatewayTimeoutCode() throws Exception {
		when(jobSearchService.search(any())).thenThrow(new JobSearchTimeoutException("timeout", new SocketTimeoutException()));

		mockMvc.perform(post("/api/jobs/search")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson()))
				.andExpect(status().isGatewayTimeout())
				.andExpect(jsonPath("$.code").value("JOB_SEARCH_TIMEOUT"));
	}

	@Test
	void invalidProviderResponseReturnsBadGatewayCode() throws Exception {
		when(jobSearchService.search(any())).thenThrow(new InvalidJobSearchResponseException("invalid"));

		mockMvc.perform(post("/api/jobs/search")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson()))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("JOB_SEARCH_INVALID_RESPONSE"));
	}

	@Test
	void configurationErrorReturnsSafeConfigurationCode() throws Exception {
		when(jobSearchService.search(any())).thenThrow(new JobSearchConfigurationException("internal config detail"));

		mockMvc.perform(post("/api/jobs/search")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson()))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("CONFIGURATION_ERROR"))
				.andExpect(jsonPath("$.message")
						.value("El servicio de busqueda de ofertas no esta configurado correctamente."));
	}

	private String validRequestJson() {
		return """
				{
				  "role": "Java Backend Developer",
				  "seniority": "JUNIOR",
				  "keywords": ["Java", "Spring Boot", "SQL", "REST API"],
				  "location": "Rosario"
				}
				""";
	}

	@TestConfiguration
	static class JobSearchTestConfiguration {
		@Bean
		JobSearchService jobSearchService() {
			return org.mockito.Mockito.mock(JobSearchService.class);
		}
	}
}
