package com.codercup.jobmatchai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codercup.jobmatchai.dto.internal.JobicySearchResponse;
import com.codercup.jobmatchai.exception.InvalidJobSearchResponseException;
import com.codercup.jobmatchai.exception.JobSearchConfigurationException;
import com.codercup.jobmatchai.exception.JobSearchTimeoutException;
import com.codercup.jobmatchai.exception.JobSearchUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class JobicyClientTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@AfterEach
	void clearInterruptFlag() {
		Thread.interrupted();
	}

	@Test
	void requestUsesGetEndpointCountIndustryAndTimeoutWithoutProfileData() {
		CapturingTransport transport = new CapturingTransport(response(200, validResponse()));
		JobicyClient client = client(transport, mutableClock());

		client.search();

		assertThat(transport.requests()).hasSize(1);
		HttpRequest request = transport.requests().get(0);
		assertThat(request.method()).isEqualTo("GET");
		assertThat(request.uri().toString()).startsWith("https://jobicy.test/api/v2/remote-jobs?");
		assertThat(decodedQuery(request)).contains("count=100");
		assertThat(decodedQuery(request)).contains("industry=engineering");
		assertThat(decodedQuery(request)).doesNotContain("geo=");
		assertThat(decodedQuery(request)).doesNotContain("tag=");
		assertThat(decodedQuery(request)).doesNotContain("role");
		assertThat(decodedQuery(request)).doesNotContain("keywords");
		assertThat(decodedQuery(request)).doesNotContain("seniority");
		assertThat(decodedQuery(request)).doesNotContain("location");
		assertThat(request.timeout()).contains(Duration.ofMillis(5000));
		assertThat(request.uri().toString()).doesNotContain("key");
	}

	@Test
	void httpsBaseUrlIsAccepted() {
		CapturingTransport transport = new CapturingTransport(response(200, validResponse()));

		new JobicyClient(objectMapper, "https://jobicy.test/api/v2/remote-jobs", 5000, 100, 60, transport,
				mutableClock()).search();

		assertThat(transport.requests()).hasSize(1);
	}

	@Test
	void httpBaseUrlIsRejected() {
		assertThatThrownBy(() -> new JobicyClient(objectMapper, "http://jobicy.test/api", 5000, 100, 60,
				new CapturingTransport(response(200, validResponse())), mutableClock()))
				.isInstanceOf(JobSearchConfigurationException.class)
				.hasMessageNotContaining("http://jobicy.test/api");
	}

	@Test
	void baseUrlWithoutHostIsRejected() {
		assertThatThrownBy(() -> new JobicyClient(objectMapper, "https:///api", 5000, 100, 60,
				new CapturingTransport(response(200, validResponse())), mutableClock()))
				.isInstanceOf(JobSearchConfigurationException.class)
				.hasMessageNotContaining("https:///api");
	}

	@Test
	void invalidConfigurationValuesAreRejected() {
		assertThatThrownBy(() -> new JobicyClient(objectMapper, "https://jobicy.test/api", 0, 100, 60,
				new CapturingTransport(response(200, validResponse())), mutableClock()))
				.isInstanceOf(JobSearchConfigurationException.class);
		assertThatThrownBy(() -> new JobicyClient(objectMapper, "https://jobicy.test/api", 5000, 101, 60,
				new CapturingTransport(response(200, validResponse())), mutableClock()))
				.isInstanceOf(JobSearchConfigurationException.class);
		assertThatThrownBy(() -> new JobicyClient(objectMapper, "https://jobicy.test/api", 5000, 100, 59,
				new CapturingTransport(response(200, validResponse())), mutableClock()))
				.isInstanceOf(JobSearchConfigurationException.class);
	}

	@Test
	void validResponseUnknownFieldsAndJobTypeArrayAreParsed() {
		JobicySearchResponse response = client(new CapturingTransport(response(200, validResponse())),
				mutableClock()).search();

		assertThat(response.jobs()).hasSize(1);
		assertThat(response.jobs().get(0).jobTitle()).isEqualTo("Java Developer");
		assertThat(response.jobs().get(0).jobIndustry().isArray()).isTrue();
		assertThat(response.jobs().get(0).jobIndustry().get(0).asText()).isEqualTo("Software Engineering");
		assertThat(response.jobs().get(0).jobType().isArray()).isTrue();
	}

	@Test
	void emptyJobsResponseIsValidAndCached() {
		CapturingTransport transport = new CapturingTransport(response(200, "{\"jobs\":[]}"));
		JobicyClient client = client(transport, mutableClock());

		assertThat(client.search().jobs()).isEmpty();
		assertThat(client.search().jobs()).isEmpty();

		assertThat(transport.requests()).hasSize(1);
	}

	@Test
	void missingJobsIsInvalidResponse() {
		assertThatThrownBy(() -> client(new CapturingTransport(response(200, "{}")), mutableClock()).search())
				.isInstanceOf(InvalidJobSearchResponseException.class);
	}

	@Test
	void invalidJsonIsInvalidResponse() {
		assertThatThrownBy(() -> client(new CapturingTransport(response(200, "{not-json")), mutableClock())
				.search())
				.isInstanceOf(InvalidJobSearchResponseException.class);
	}

	@Test
	void providerErrorsAreUnavailable() {
		assertThatThrownBy(() -> client(new CapturingTransport(response(429, "{}")), mutableClock()).search())
				.isInstanceOf(JobSearchUnavailableException.class);
		assertThatThrownBy(() -> client(new CapturingTransport(response(503, "{}")), mutableClock()).search())
				.isInstanceOf(JobSearchUnavailableException.class);
	}

	@Test
	void ioTimeoutAndInterruptedAreMappedSafely() {
		assertThatThrownBy(() -> client(request -> {
			throw new IOException("network");
		}, mutableClock()).search()).isInstanceOf(JobSearchUnavailableException.class);

		assertThatThrownBy(() -> client(request -> {
			throw new HttpTimeoutException("timeout");
		}, mutableClock()).search()).isInstanceOf(JobSearchTimeoutException.class);

		assertThatThrownBy(() -> client(request -> {
			throw new InterruptedException("interrupted");
		}, mutableClock()).search()).isInstanceOf(JobSearchUnavailableException.class);
		assertThat(Thread.currentThread().isInterrupted()).isTrue();
	}

	@Test
	void cacheUsesOneProviderCallInsideTtlAndRefreshesAfterTtl() {
		MutableClock clock = mutableClock();
		CapturingTransport transport = new CapturingTransport(response(200, validResponse()), response(200, secondResponse()));
		JobicyClient client = client(transport, clock);

		client.search();
		client.search();
		clock.advance(Duration.ofMinutes(61));
		JobicySearchResponse refreshed = client.search();

		assertThat(transport.requests()).hasSize(2);
		assertThat(refreshed.jobs().get(0).jobTitle()).isEqualTo("Spring Engineer");
	}

	@Test
	void oneGlobalCacheIsUsedRegardlessOfSearchContext() {
		CapturingTransport transport = new CapturingTransport(
				response(200, validResponse()),
				response(200, secondResponse())
		);
		JobicyClient client = client(transport, mutableClock());

		client.search();
		client.search();
		client.search();

		assertThat(transport.requests()).hasSize(1);
		assertThat(decodedQuery(transport.requests().get(0))).doesNotContain("geo=");
	}

	@Test
	void staleCacheIsUsedWhenRefreshUnavailableButFirstLoadFailurePropagates() {
		MutableClock clock = mutableClock();
		CapturingTransport transport = new CapturingTransport(response(200, validResponse()), response(503, "{}"));
		JobicyClient client = client(transport, clock);

		client.search();
		clock.advance(Duration.ofMinutes(61));
		JobicySearchResponse stale = client.search();

		assertThat(stale.jobs().get(0).jobTitle()).isEqualTo("Java Developer");
		assertThat(transport.requests()).hasSize(2);

		assertThatThrownBy(() -> client(new CapturingTransport(response(503, "{}")), mutableClock()).search())
				.isInstanceOf(JobSearchUnavailableException.class);
	}

	@Test
	void failedRefreshExtendsStaleCacheProtectionForAnotherTtl() {
		MutableClock clock = mutableClock();
		CapturingTransport transport = new CapturingTransport(
				response(200, validResponse()),
				response(503, "{}"),
				response(200, secondResponse())
		);
		JobicyClient client = client(transport, clock);

		client.search();
		assertThat(transport.requests()).hasSize(1);

		clock.advance(Duration.ofMinutes(61));
		assertThat(client.search().jobs().get(0).jobTitle()).isEqualTo("Java Developer");
		assertThat(transport.requests()).hasSize(2);

		assertThat(client.search().jobs().get(0).jobTitle()).isEqualTo("Java Developer");
		assertThat(transport.requests()).hasSize(2);

		clock.advance(Duration.ofMinutes(59));
		assertThat(client.search().jobs().get(0).jobTitle()).isEqualTo("Java Developer");
		assertThat(transport.requests()).hasSize(2);

		clock.advance(Duration.ofMinutes(2));
		assertThat(client.search().jobs().get(0).jobTitle()).isEqualTo("Spring Engineer");
		assertThat(transport.requests()).hasSize(3);
	}

	@Test
	void staleCacheIsUsedWhenRefreshTimesOut() {
		MutableClock clock = mutableClock();
		CapturingTransport transport = new CapturingTransport(response(200, validResponse()));
		JobicyClient client = client(request -> {
			if (transport.requests().isEmpty()) {
				return transport.send(request);
			}
			throw new HttpTimeoutException("timeout");
		}, clock);

		client.search();
		clock.advance(Duration.ofMinutes(61));
		JobicySearchResponse stale = client.search();

		assertThat(stale.jobs().get(0).jobTitle()).isEqualTo("Java Developer");
	}

	@Test
	void staleCacheIsUsedWhenRefreshResponseIsInvalid() {
		MutableClock clock = mutableClock();
		CapturingTransport invalidJsonTransport = new CapturingTransport(response(200, validResponse()), response(200, "{not-json"));
		JobicyClient invalidJsonClient = client(invalidJsonTransport, clock);

		invalidJsonClient.search();
		clock.advance(Duration.ofMinutes(61));
		assertThat(invalidJsonClient.search().jobs().get(0).jobTitle()).isEqualTo("Java Developer");

		MutableClock secondClock = mutableClock();
		CapturingTransport missingJobsTransport = new CapturingTransport(response(200, validResponse()), response(200, "{}"));
		JobicyClient missingJobsClient = client(missingJobsTransport, secondClock);

		missingJobsClient.search();
		secondClock.advance(Duration.ofMinutes(61));
		assertThat(missingJobsClient.search().jobs().get(0).jobTitle()).isEqualTo("Java Developer");
	}

	private JobicyClient client(JobicyClient.JobicyTransport transport, Clock clock) {
		return new JobicyClient(objectMapper, "https://jobicy.test/api/v2/remote-jobs", 5000, 100, 60, transport, clock);
	}

	private String decodedQuery(HttpRequest request) {
		return URLDecoder.decode(request.uri().getRawQuery(), StandardCharsets.UTF_8);
	}

	private JobicyClient.JobicyHttpResponse response(int statusCode, String body) {
		return new JobicyClient.JobicyHttpResponse(statusCode, body);
	}

	private String validResponse() {
		return """
				{
				  "ignored": true,
				  "jobs": [
				    {
				      "id": 123,
				      "url": "https://example.com/job/123",
				      "jobTitle": "Java Developer",
				      "companyName": "Example",
				      "jobIndustry": ["Software Engineering"],
				      "jobType": ["full-time", "contract"],
				      "jobDescription": "<p>Java and Spring Boot</p>",
				      "pubDate": "2026-08-16T15:30:00Z"
				    }
				  ]
				}
				""";
	}

	private String secondResponse() {
		return """
				{
				  "jobs": [
				    {
				      "id": 456,
				      "url": "https://example.com/job/456",
				      "jobTitle": "Spring Engineer"
				    }
				  ]
				}
				""";
	}

	private MutableClock mutableClock() {
		return new MutableClock(Instant.parse("2026-08-16T00:00:00Z"));
	}

	private static class CapturingTransport implements JobicyClient.JobicyTransport {
		private final List<JobicyClient.JobicyHttpResponse> responses;
		private final List<HttpRequest> requests = new ArrayList<>();

		CapturingTransport(JobicyClient.JobicyHttpResponse... responses) {
			this.responses = new ArrayList<>(List.of(responses));
		}

		List<HttpRequest> requests() {
			return requests;
		}

		@Override
		public JobicyClient.JobicyHttpResponse send(HttpRequest request) {
			requests.add(request);
			return responses.isEmpty() ? new JobicyClient.JobicyHttpResponse(200, "{\"jobs\":[]}") : responses.remove(0);
		}
	}

	private static class MutableClock extends Clock {
		private Instant instant;

		MutableClock(Instant instant) {
			this.instant = instant;
		}

		void advance(Duration duration) {
			instant = instant.plus(duration);
		}

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
