package com.codercup.jobmatchai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codercup.jobmatchai.dto.AnalysisResponse;
import com.codercup.jobmatchai.service.GeminiService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ActuatorHealthEndpointTest.TestGeminiConfiguration.class)
class ActuatorHealthEndpointTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthEndpointReturnsUp() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@TestConfiguration
	static class TestGeminiConfiguration {

		@Bean
		GeminiService geminiService() {
			return new GeminiService("test-key", "test-model", 30000, 2, 500) {
				@Override
				public AnalysisResponse analyze(String cvText, String jobDescription) {
					return new AnalysisResponse(0, List.of(), List.of(), List.of(), List.of());
				}

				@Override
				public AnalysisResponse analyze(String cvText, MultipartFile jobImage) {
					return new AnalysisResponse(0, List.of(), List.of(), List.of(), List.of());
				}
			};
		}
	}
}
