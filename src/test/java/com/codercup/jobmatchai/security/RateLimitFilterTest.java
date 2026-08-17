package com.codercup.jobmatchai.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitFilterTest {

	private static final int REQUESTS_PER_MINUTE = 10;
	private final RateLimitFilter filter = new RateLimitFilter(REQUESTS_PER_MINUTE);

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void authenticatedUserIsIdentifiedByUsername() throws Exception {
		authenticateAs("demo");

		for (int index = 0; index < REQUESTS_PER_MINUTE; index++) {
			MockHttpServletResponse response = performAnalyzeRequest("10.0.0." + index, null);
			assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
		}

		MockHttpServletResponse response = performAnalyzeRequest("10.0.0.99", null);

		assertTooManyRequests(response);
	}

	@Test
	void anonymousUserWithCfConnectingIpIsIdentifiedByHeader() throws Exception {
		authenticateAnonymously();

		for (int index = 0; index < REQUESTS_PER_MINUTE; index++) {
			MockHttpServletResponse response = performAnalyzeRequest("10.0.0." + index, "203.0.113.10");
			assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
		}

		MockHttpServletResponse response = performAnalyzeRequest("10.0.0.99", "203.0.113.10");

		assertTooManyRequests(response);
	}

	@Test
	void cfConnectingIpIsTrimmedForAnonymousUsers() throws Exception {
		authenticateAnonymously();

		for (int index = 0; index < REQUESTS_PER_MINUTE; index++) {
			MockHttpServletResponse response = performAnalyzeRequest("10.0.0." + index, " 203.0.113.10 ");
			assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
		}

		MockHttpServletResponse response = performAnalyzeRequest("10.0.0.99", "203.0.113.10");

		assertTooManyRequests(response);
	}

	@Test
	void anonymousUserWithoutCfConnectingIpFallsBackToRemoteAddr() throws Exception {
		authenticateAnonymously();

		for (int index = 0; index < REQUESTS_PER_MINUTE; index++) {
			MockHttpServletResponse response = performAnalyzeRequest("198.51.100.25", null);
			assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
		}

		MockHttpServletResponse response = performAnalyzeRequest("198.51.100.25", null);

		assertTooManyRequests(response);
	}

	@Test
	void tenRequestsFromSameIpAreAllowedAndEleventhIsRejected() throws Exception {
		authenticateAnonymously();

		for (int index = 0; index < REQUESTS_PER_MINUTE; index++) {
			MockHttpServletResponse response = performAnalyzeRequest("198.51.100.25", "203.0.113.10");
			assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
		}

		MockHttpServletResponse response = performAnalyzeRequest("198.51.100.25", "203.0.113.10");

		assertTooManyRequests(response);
	}

	@Test
	void jobsEndpointHasIndependentBucketFromAnalyze() throws Exception {
		authenticateAnonymously();

		for (int index = 0; index < REQUESTS_PER_MINUTE; index++) {
			assertThat(performJobsRequest("198.51.100.25", "203.0.113.10").getStatus())
					.isEqualTo(HttpStatus.OK.value());
		}

		assertTooManyRequests(performJobsRequest("198.51.100.25", "203.0.113.10"));
		assertThat(performAnalyzeRequest("198.51.100.25", "203.0.113.10").getStatus())
				.isEqualTo(HttpStatus.OK.value());
	}

	@Test
	void analyzeEndpointHasIndependentBucketFromJobs() throws Exception {
		authenticateAnonymously();

		for (int index = 0; index < REQUESTS_PER_MINUTE; index++) {
			assertThat(performAnalyzeRequest("198.51.100.25", "203.0.113.10").getStatus())
					.isEqualTo(HttpStatus.OK.value());
		}

		assertTooManyRequests(performAnalyzeRequest("198.51.100.25", "203.0.113.10"));
		assertThat(performJobsRequest("198.51.100.25", "203.0.113.10").getStatus())
				.isEqualTo(HttpStatus.OK.value());
	}

	@Test
	void differentIpHasIndependentCounter() throws Exception {
		authenticateAnonymously();

		for (int index = 0; index < REQUESTS_PER_MINUTE; index++) {
			MockHttpServletResponse response = performAnalyzeRequest("198.51.100.25", "203.0.113.10");
			assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
		}

		MockHttpServletResponse response = performAnalyzeRequest("198.51.100.25", "203.0.113.11");

		assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
	}

	@Test
	void tooManyRequestsResponseIncludesRetryAfterAndMessage() throws Exception {
		authenticateAnonymously();

		for (int index = 0; index < REQUESTS_PER_MINUTE; index++) {
			performAnalyzeRequest("198.51.100.25", "203.0.113.10");
		}

		MockHttpServletResponse response = performAnalyzeRequest("198.51.100.25", "203.0.113.10");

		assertAnalyzeTooManyRequests(response);
	}

	@Test
	void jobsTooManyRequestsResponseIncludesSpecificMessage() throws Exception {
		authenticateAnonymously();

		for (int index = 0; index < REQUESTS_PER_MINUTE; index++) {
			performJobsRequest("198.51.100.25", "203.0.113.10");
		}

		MockHttpServletResponse response = performJobsRequest("198.51.100.25", "203.0.113.10");

		assertTooManyRequests(response);
		assertThat(response.getContentAsString()).contains("Se supero el limite de busquedas de ofertas por minuto.");
	}

	private MockHttpServletResponse performAnalyzeRequest(String remoteAddr, String cfConnectingIp)
			throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/analyze");
		return performRequest(request, remoteAddr, cfConnectingIp);
	}

	private MockHttpServletResponse performJobsRequest(String remoteAddr, String cfConnectingIp)
			throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/jobs/search");
		return performRequest(request, remoteAddr, cfConnectingIp);
	}

	private MockHttpServletResponse performRequest(MockHttpServletRequest request, String remoteAddr, String cfConnectingIp)
			throws ServletException, IOException {
		request.setRemoteAddr(remoteAddr);
		if (cfConnectingIp != null) {
			request.addHeader("CF-Connecting-IP", cfConnectingIp);
		}
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		return response;
	}

	private void authenticateAs(String username) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(username, "password", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	private void authenticateAnonymously() {
		SecurityContextHolder.getContext().setAuthentication(
				new AnonymousAuthenticationToken("key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
	}

	private void assertTooManyRequests(MockHttpServletResponse response) throws Exception {
		assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
		assertThat(response.getHeader("Retry-After")).isEqualTo("60");
		assertThat(response.getContentAsString()).contains("\"code\":\"RATE_LIMIT_EXCEEDED\"");
	}

	private void assertAnalyzeTooManyRequests(MockHttpServletResponse response) throws Exception {
		assertTooManyRequests(response);
		assertThat(response.getContentAsString()).contains("Se supero el limite de analisis por minuto.");
	}
}
