package com.codercup.jobmatchai.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"security.enabled=true",
		"security.demo-username=demo",
		"security.demo-password=demo-password",
		"rate-limit.per-minute=1"
})
@AutoConfigureMockMvc
class RateLimitFilterIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void publicAnalyzeEndpointIsConnectedToRateLimitFilter() throws Exception {
		mockMvc.perform(multipart("/api/analyze"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(multipart("/api/analyze"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
				.andExpect(jsonPath("$.message").value("Se supero el limite de analisis por minuto."));
	}
}
