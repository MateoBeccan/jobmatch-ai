package com.codercup.jobmatchai.client;

import com.codercup.jobmatchai.dto.internal.JobicySearchResponse;
import com.codercup.jobmatchai.exception.InvalidJobSearchResponseException;
import com.codercup.jobmatchai.exception.JobSearchConfigurationException;
import com.codercup.jobmatchai.exception.JobSearchTimeoutException;
import com.codercup.jobmatchai.exception.JobSearchUnavailableException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JobicyClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(JobicyClient.class);
	private static final String INVALID_RESPONSE_MESSAGE = "No pudimos interpretar las ofertas recibidas.";
	private static final String UNAVAILABLE_MESSAGE = "El servicio de busqueda de ofertas no esta disponible en este momento.";
	private static final String TIMEOUT_MESSAGE = "La busqueda de ofertas tardo demasiado en responder.";

	private final ObjectMapper objectMapper;
	private final URI baseUri;
	private final Duration timeout;
	private final int resultLimit;
	private final Duration cacheTtl;
	private final JobicyTransport transport;
	private final Clock clock;
	private CacheEntry cache;

	@Autowired
	public JobicyClient(
			ObjectMapper objectMapper,
			@Value("${jobicy.base-url:https://jobicy.com/api/v2/remote-jobs}") String baseUrl,
			@Value("${jobicy.timeout-ms:5000}") int timeoutMs,
			@Value("${jobicy.result-limit:100}") int resultLimit,
			@Value("${jobicy.cache-ttl-minutes:60}") int cacheTtlMinutes
	) {
		this(objectMapper, baseUrl, timeoutMs, resultLimit, cacheTtlMinutes,
				new JavaHttpJobicyTransport(HttpClient.newHttpClient()), Clock.systemUTC());
	}

	JobicyClient(
			ObjectMapper objectMapper,
			String baseUrl,
			int timeoutMs,
			int resultLimit,
			int cacheTtlMinutes,
			JobicyTransport transport,
			Clock clock
	) {
		if (timeoutMs <= 0) {
			throw new JobSearchConfigurationException("La configuracion de Jobicy no es valida.");
		}
		if (resultLimit < 1 || resultLimit > 100) {
			throw new JobSearchConfigurationException("La configuracion de Jobicy no es valida.");
		}
		if (cacheTtlMinutes < 60) {
			throw new JobSearchConfigurationException("La configuracion de Jobicy no es valida.");
		}
		this.objectMapper = objectMapper;
		this.baseUri = validateBaseUrl(baseUrl);
		this.timeout = Duration.ofMillis(timeoutMs);
		this.resultLimit = resultLimit;
		this.cacheTtl = Duration.ofMinutes(cacheTtlMinutes);
		this.transport = transport;
		this.clock = clock;
	}

	public synchronized JobicySearchResponse search() {
		CacheEntry current = cache;
		Instant now = clock.instant();
		if (current != null && current.isFresh(now, cacheTtl)) {
			return current.response();
		}

		try {
			JobicySearchResponse fresh = fetch();
			cache = new CacheEntry(fresh, now);
			return fresh;
		}
		catch (InvalidJobSearchResponseException | JobSearchUnavailableException | JobSearchTimeoutException exception) {
			if (current != null) {
				cache = new CacheEntry(current.response(), now);
				return current.response();
			}
			throw exception;
		}
	}

	private JobicySearchResponse fetch() {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(buildUri())
					.timeout(timeout)
					.GET()
					.build();
			JobicyHttpResponse response = transport.send(request);
			return handleResponse(response);
		}
		catch (HttpTimeoutException exception) {
			throw new JobSearchTimeoutException(TIMEOUT_MESSAGE, exception);
		}
		catch (IOException exception) {
			throw new JobSearchUnavailableException(UNAVAILABLE_MESSAGE, exception);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new JobSearchUnavailableException(UNAVAILABLE_MESSAGE, exception);
		}
	}

	private URI buildUri() {
		StringBuilder query = new StringBuilder();
		appendQueryParam(query, "count", String.valueOf(resultLimit));
		appendQueryParam(query, "industry", "engineering");

		String separator = baseUri.getRawQuery() == null ? "?" : "&";
		return URI.create(baseUri + separator + query);
	}

	private void appendQueryParam(StringBuilder query, String name, String value) {
		if (!query.isEmpty()) {
			query.append('&');
		}
		query.append(URLEncoder.encode(name, StandardCharsets.UTF_8));
		query.append('=');
		query.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
	}

	private JobicySearchResponse handleResponse(JobicyHttpResponse response) {
		int statusCode = response.statusCode();
		if (statusCode == 200) {
			try {
				JobicySearchResponse parsed = objectMapper.readValue(response.body(), JobicySearchResponse.class);
				if (parsed.jobs() == null) {
					throw new InvalidJobSearchResponseException(INVALID_RESPONSE_MESSAGE);
				}
				return parsed;
			}
			catch (JsonProcessingException exception) {
				throw new InvalidJobSearchResponseException(INVALID_RESPONSE_MESSAGE, exception);
			}
		}

		LOGGER.warn("Jobicy request failed with status {}", statusCode);
		throw new JobSearchUnavailableException(UNAVAILABLE_MESSAGE);
	}

	private URI validateBaseUrl(String baseUrl) {
		String normalized = baseUrl == null ? "" : baseUrl.strip();
		try {
			URI uri = URI.create(normalized);
			if (!uri.isAbsolute()
					|| !"https".equalsIgnoreCase(uri.getScheme())
					|| uri.getHost() == null
					|| uri.getHost().isBlank()) {
				throw new JobSearchConfigurationException("La configuracion de Jobicy no es valida.");
			}
			return uri;
		}
		catch (IllegalArgumentException exception) {
			throw new JobSearchConfigurationException("La configuracion de Jobicy no es valida.");
		}
	}

	private record CacheEntry(JobicySearchResponse response, Instant fetchedAt) {
		private boolean isFresh(Instant now, Duration ttl) {
			return fetchedAt.plus(ttl).isAfter(now);
		}
	}

	record JobicyHttpResponse(int statusCode, String body) {
	}

	@FunctionalInterface
	interface JobicyTransport {
		JobicyHttpResponse send(HttpRequest request) throws IOException, InterruptedException;
	}

	private static final class JavaHttpJobicyTransport implements JobicyTransport {
		private final HttpClient httpClient;

		private JavaHttpJobicyTransport(HttpClient httpClient) {
			this.httpClient = httpClient;
		}

		@Override
		public JobicyHttpResponse send(HttpRequest request) throws IOException, InterruptedException {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			return new JobicyHttpResponse(response.statusCode(), response.body());
		}
	}
}
