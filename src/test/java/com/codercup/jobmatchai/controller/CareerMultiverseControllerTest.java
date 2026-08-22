package com.codercup.jobmatchai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codercup.jobmatchai.dto.career.CareerMarketConfidence;
import com.codercup.jobmatchai.dto.career.CareerMarketRequest;
import com.codercup.jobmatchai.dto.career.CareerMarketResponse;
import com.codercup.jobmatchai.dto.career.CareerMultiverseRequest;
import com.codercup.jobmatchai.dto.career.CareerMultiverseResponse;
import com.codercup.jobmatchai.dto.career.CareerPathMarketResponse;
import com.codercup.jobmatchai.dto.career.CareerPathResponse;
import com.codercup.jobmatchai.dto.career.CareerPathType;
import com.codercup.jobmatchai.dto.career.CareerProfileResponse;
import com.codercup.jobmatchai.dto.career.CareerRegion;
import com.codercup.jobmatchai.dto.career.CareerSkillDemandResponse;
import com.codercup.jobmatchai.exception.ApiExceptionHandler;
import com.codercup.jobmatchai.exception.InvalidCareerMarketRequestException;
import com.codercup.jobmatchai.exception.InvalidCareerMultiverseRequestException;
import com.codercup.jobmatchai.service.CareerMarketService;
import com.codercup.jobmatchai.service.CareerMultiverseService;
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

@WebMvcTest(
		value = CareerMultiverseController.class,
		properties = {
				"rate-limit.per-minute=100",
				"rate-limit.career-per-minute=100"
		}
)
@Import({ApiExceptionHandler.class, CareerMultiverseControllerTest.CareerMarketTestConfiguration.class})
class CareerMultiverseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CareerMarketService careerMarketService;

	@Autowired
	private CareerMultiverseService careerMultiverseService;

	@BeforeEach
	void resetMock() {
		reset(careerMarketService);
		reset(careerMultiverseService);
	}

	@Test
	void marketReturnsOkForValidRequest() throws Exception {
		when(careerMarketService.analyze(any(CareerMarketRequest.class))).thenReturn(new CareerMarketResponse(
				"JOBICY",
				"Java Backend Developer",
				CareerRegion.LATAM,
				12,
				CareerMarketConfidence.HIGH,
				71,
				List.of("Java", "Spring Boot"),
				List.of(new CareerSkillDemandResponse("Docker", 8, 67)),
				List.of(new CareerSkillDemandResponse("Java", 12, 100))
		));

		mockMvc.perform(post("/api/career/market")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.provider").value("JOBICY"))
				.andExpect(jsonPath("$.sampleSize").value(12))
				.andExpect(jsonPath("$.confidence").value("HIGH"))
				.andExpect(jsonPath("$.coveragePercentage").value(71))
				.andExpect(jsonPath("$.currentSkillsDetected[0]").value("Java"))
				.andExpect(jsonPath("$.missingSkills[0].skill").value("Docker"))
				.andExpect(jsonPath("$.skillDemand[0].skill").value("Java"));
	}

	@Test
	void invalidRequestsReturnBadRequestCode() throws Exception {
		when(careerMarketService.analyze(any())).thenThrow(new InvalidCareerMarketRequestException());

		for (String body : List.of(
				"{}",
				"{\"role\":\" \",\"seniority\":\"JUNIOR\",\"currentSkills\":[\"Java\"],\"region\":\"LATAM\"}",
				"{\"role\":\"%s\",\"seniority\":\"JUNIOR\",\"currentSkills\":[\"Java\"],\"region\":\"LATAM\"}"
						.formatted("A".repeat(81)),
				"{\"role\":\"Java\",\"currentSkills\":[\"Java\"],\"region\":\"LATAM\"}",
				"{\"role\":\"Java\",\"seniority\":\"JUNIOR\",\"currentSkills\":[],\"region\":\"LATAM\"}",
				"{\"role\":\"Java\",\"seniority\":\"JUNIOR\",\"currentSkills\":[%s],\"region\":\"LATAM\"}"
						.formatted("\"Java\",".repeat(20) + "\"SQL\""),
				"{\"role\":\"Java\",\"seniority\":\"JUNIOR\",\"currentSkills\":[\"%s\"],\"region\":\"LATAM\"}"
						.formatted("A".repeat(51)),
				"{\"role\":\"Java\",\"seniority\":\"JUNIOR\",\"currentSkills\":[\"Java\"]}"
		)) {
			mockMvc.perform(post("/api/career/market")
							.contentType(MediaType.APPLICATION_JSON)
							.content(body))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_CAREER_MARKET_REQUEST"));
		}
	}

	@Test
	void malformedJsonReturnsCareerMarketRequestCode() throws Exception {
		mockMvc.perform(post("/api/career/market")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{not-json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_CAREER_MARKET_REQUEST"));
	}

	@Test
	void multiverseReturnsOkForValidRequest() throws Exception {
		when(careerMultiverseService.generate(any(CareerMultiverseRequest.class))).thenReturn(new CareerMultiverseResponse(
				"JOBICY",
				CareerRegion.LATAM,
				new CareerProfileResponse("Java Backend Developer", com.codercup.jobmatchai.dto.JobSeniority.JUNIOR,
						List.of("Java", "Spring Boot")),
				List.of(new CareerPathResponse(
						CareerPathType.NATURAL,
						"Java Backend Developer",
						"Summary",
						"Rationale",
						new CareerPathMarketResponse(
								12,
								CareerMarketConfidence.HIGH,
								71,
								List.of("Java"),
								List.of(new CareerSkillDemandResponse("Docker", 8, 67)),
								List.of(new CareerSkillDemandResponse("Java", 12, 100))
						),
						List.of(),
						List.of(),
						null
				))
		));

		mockMvc.perform(post("/api/career/multiverse")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validMultiverseRequestJson()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.provider").value("JOBICY"))
				.andExpect(jsonPath("$.region").value("LATAM"))
				.andExpect(jsonPath("$.profile.role").value("Java Backend Developer"))
				.andExpect(jsonPath("$.paths[0].type").value("NATURAL"))
				.andExpect(jsonPath("$.paths[0].market.coveragePercentage").value(71));
	}

	@Test
	void invalidMultiverseRequestsReturnBadRequestCode() throws Exception {
		when(careerMultiverseService.generate(any())).thenThrow(new InvalidCareerMultiverseRequestException());

		for (String body : List.of(
				"{}",
				"{\"role\":\" \",\"seniority\":\"JUNIOR\",\"skills\":[\"Java\"],\"region\":\"LATAM\"}",
				"{\"role\":\"%s\",\"seniority\":\"JUNIOR\",\"skills\":[\"Java\"],\"region\":\"LATAM\"}"
						.formatted("A".repeat(81)),
				"{\"role\":\"Java\",\"seniority\":\"JUNIOR\",\"region\":\"LATAM\"}",
				"{\"role\":\"Java\",\"seniority\":\"JUNIOR\",\"skills\":[],\"region\":\"LATAM\"}",
				"{\"role\":\"Java\",\"seniority\":\"JUNIOR\",\"skills\":[%s],\"region\":\"LATAM\"}"
						.formatted("\"Java\",".repeat(20) + "\"SQL\""),
				"{\"role\":\"Java\",\"seniority\":\"JUNIOR\",\"skills\":[\" \"],\"region\":\"LATAM\"}",
				"{\"role\":\"Java\",\"seniority\":\"JUNIOR\",\"skills\":[\"%s\"],\"region\":\"LATAM\"}"
						.formatted("A".repeat(51)),
				"{\"role\":\"Java\",\"skills\":[\"Java\"],\"region\":\"LATAM\"}",
				"{\"role\":\"Java\",\"seniority\":\"JUNIOR\",\"skills\":[\"Java\"]}"
		)) {
			mockMvc.perform(post("/api/career/multiverse")
							.contentType(MediaType.APPLICATION_JSON)
							.content(body))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_CAREER_MULTIVERSE_REQUEST"));
		}
	}

	@Test
	void malformedJsonReturnsCareerMultiverseRequestCode() throws Exception {
		mockMvc.perform(post("/api/career/multiverse")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{not-json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_CAREER_MULTIVERSE_REQUEST"));
	}

	private String validRequestJson() {
		return """
				{
				  "role": "Java Backend Developer",
				  "seniority": "JUNIOR",
				  "currentSkills": ["Java", "Spring Boot", "SQL", "REST APIs", "Git"],
				  "region": "LATAM"
				}
				""";
	}

	private String validMultiverseRequestJson() {
		return """
				{
				  "role": "Java Backend Developer",
				  "seniority": "JUNIOR",
				  "skills": ["Java", "Spring Boot", "SQL", "REST APIs", "Git"],
				  "region": "LATAM"
				}
				""";
	}

	@TestConfiguration
	static class CareerMarketTestConfiguration {
		@Bean
		CareerMarketService careerMarketService() {
			return org.mockito.Mockito.mock(CareerMarketService.class);
		}

		@Bean
		CareerMultiverseService careerMultiverseService() {
			return org.mockito.Mockito.mock(CareerMultiverseService.class);
		}
	}
}
