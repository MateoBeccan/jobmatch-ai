package com.codercup.jobmatchai.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"security.enabled=false",
		"rate-limit.per-minute=10"
})
@AutoConfigureMockMvc
class SecurityDisabledConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void historyIsPublicWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/analyses"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
	}

	@Test
	void createAnalysisIsPublicWithoutAuthentication() throws Exception {
		mockMvc.perform(multipart("/api/analyses"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Falta información requerida para procesar la solicitud."));
	}

	@Test
	void deleteAnalysisIsPublicWithoutAuthentication() throws Exception {
		mockMvc.perform(delete("/api/analyses/inexistente"))
				.andExpect(status().isNotFound());
	}
}
