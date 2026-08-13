package com.codercup.jobmatchai.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"security.enabled=true",
		"security.demo-username=demo",
		"security.demo-password=demo-password"
})
@AutoConfigureMockMvc
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/analyses"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanReadOwnHistory() throws Exception {
		mockMvc.perform(get("/api/analyses")
					.header(HttpHeaders.AUTHORIZATION, basicCredentials("demo", "demo-password")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
	}

	private String basicCredentials(String username, String password) {
		String credentials = username + ":" + password;
		return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
	}
}
