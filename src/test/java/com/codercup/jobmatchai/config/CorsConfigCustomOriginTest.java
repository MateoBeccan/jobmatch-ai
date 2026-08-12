package com.codercup.jobmatchai.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "cors.allowed-origins=https://jobmatch-test.example, http://localhost:5173")
@AutoConfigureMockMvc
class CorsConfigCustomOriginTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void preflightAllowsCustomOriginConfiguredByProperty() throws Exception {
		mockMvc.perform(options("/api/analyze")
						.header(HttpHeaders.ORIGIN, "https://jobmatch-test.example")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://jobmatch-test.example"));
	}
}
