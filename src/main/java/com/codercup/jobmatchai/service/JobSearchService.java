package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.client.JobicyClient;
import com.codercup.jobmatchai.dto.JobOfferResponse;
import com.codercup.jobmatchai.dto.JobSearchRequest;
import com.codercup.jobmatchai.dto.JobSearchResponse;
import com.codercup.jobmatchai.dto.JobSeniority;
import com.codercup.jobmatchai.dto.internal.JobicyJob;
import com.codercup.jobmatchai.dto.internal.JobicySearchResponse;
import com.codercup.jobmatchai.exception.InvalidJobSearchRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.net.URI;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class JobSearchService {

	private static final int MAX_ROLE_LENGTH = 80;
	private static final int MIN_KEYWORDS = 3;
	private static final int MAX_KEYWORDS = 6;
	private static final int MAX_KEYWORD_LENGTH = 50;
	private static final int MAX_LOCATION_LENGTH = 120;
	private static final int MAX_DESCRIPTION_SNIPPET_LENGTH = 300;
	private static final Pattern UNSAFE_TAG_PATTERN = Pattern.compile("<[^>]*>");
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
	private static final Pattern OBVIOUS_SENIORITY_PATTERN = Pattern.compile(
			"(?i)(^|[^a-z0-9])(senior|sr\\.?|lead|staff|principal)([^a-z0-9]|$)"
	);
	private static final Pattern SENIOR_JOB_LEVEL_PATTERN = Pattern.compile(
			"(?i)(^|[^a-z0-9])(senior|midweight|director|manager|executive)([^a-z0-9]|$)"
	);
	private static final Set<String> GENERIC_ROLE_TOKENS = Set.of(
			"developer", "developers", "engineer", "engineers", "software", "programmer", "programmers",
			"specialist", "specialists"
	);

	private final JobicyClient jobicyClient;
	private final int maxResults;

	public JobSearchService(
			JobicyClient jobicyClient,
			@Value("${job-search.max-results:8}") int maxResults
	) {
		if (maxResults < 1) {
			throw new IllegalArgumentException("La configuracion de busqueda laboral debe ser mayor a 0.");
		}
		this.jobicyClient = jobicyClient;
		this.maxResults = maxResults;
	}

	public JobSearchResponse search(JobSearchRequest request) {
		NormalizedJobSearchRequest normalized = validateAndNormalize(request);
		JobicySearchResponse providerResponse = jobicyClient.search();
		List<JobOfferResponse> jobs = normalizeJobs(providerResponse.jobs(), normalized).stream()
				.sorted(Comparator
						.comparingInt((JobOfferResponse job) -> job.matchedKeywords().size()).reversed()
						.thenComparing((JobOfferResponse job) -> parseUpdatedAt(job.updatedAt()),
								Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(maxResults)
				.toList();
		return new JobSearchResponse("JOBICY", jobs.size(), jobs);
	}

	private NormalizedJobSearchRequest validateAndNormalize(JobSearchRequest request) {
		if (request == null || request.role() == null || request.seniority() == null
				|| request.keywords() == null || request.location() == null) {
			throw new InvalidJobSearchRequestException();
		}

		String role = request.role().trim();
		String location = request.location().trim();
		if (role.isBlank() || role.length() > MAX_ROLE_LENGTH
				|| location.isBlank() || location.length() > MAX_LOCATION_LENGTH
				|| request.keywords().size() < MIN_KEYWORDS || request.keywords().size() > MAX_KEYWORDS) {
			throw new InvalidJobSearchRequestException();
		}

		List<String> keywords = normalizeKeywords(request.keywords());
		if (keywords.size() < MIN_KEYWORDS) {
			throw new InvalidJobSearchRequestException();
		}

		return new NormalizedJobSearchRequest(role, request.seniority(), keywords, location);
	}

	private List<String> normalizeKeywords(List<String> keywords) {
		List<String> normalized = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (String keyword : keywords) {
			if (keyword == null) {
				throw new InvalidJobSearchRequestException();
			}
			String trimmed = keyword.trim();
			if (trimmed.isBlank() || trimmed.length() > MAX_KEYWORD_LENGTH) {
				throw new InvalidJobSearchRequestException();
			}
			if (seen.add(trimmed.toLowerCase(Locale.ROOT))) {
				normalized.add(trimmed);
			}
		}
		return List.copyOf(normalized);
	}

	private LocationScope resolveScope(String location) {
		String normalized = normalizeForGeo(location);
		if ("argentina".equals(normalized) || "ar".equals(normalized)) {
			return LocationScope.ARGENTINA;
		}
		if ("latam".equals(normalized)
				|| "latin america".equals(normalized)
				|| "america latina".equals(normalized)) {
			return LocationScope.LATAM;
		}
		return LocationScope.GLOBAL;
	}

	private String normalizeForGeo(String value) {
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT);
		return WHITESPACE_PATTERN.matcher(normalized.trim()).replaceAll(" ");
	}

	private List<JobOfferResponse> normalizeJobs(List<JobicyJob> jobs, NormalizedJobSearchRequest request) {
		if (jobs == null || jobs.isEmpty()) {
			return List.of();
		}

		return jobs.stream()
				.map(job -> normalizeJob(job, request))
				.filter(Objects::nonNull)
				.toList();
	}

	private JobOfferResponse normalizeJob(JobicyJob job, NormalizedJobSearchRequest request) {
		if (job == null) {
			return null;
		}

		String title = trimToNull(job.jobTitle());
		String url = safeHttpsUrl(job.url());
		if (title == null || url == null
				|| !passesGeoGate(job.jobGeo(), resolveScope(request.location()))
				|| isObviousSeniorityMismatch(title, job.jobLevel(), request.seniority())) {
			return null;
		}

		String descriptionText = toPlainText(job.jobDescription());
		List<String> matchedKeywords = matchedKeywords(title + " " + (descriptionText == null ? "" : descriptionText),
				request.keywords());
		if (!passesRelevanceGate(title, matchedKeywords, request.keywords(), significantRoleTokens(request.role()))) {
			return null;
		}

		String snippet = normalizeSnippet(job.jobExcerpt(), descriptionText);
		return new JobOfferResponse(
				job.id() == null ? null : String.valueOf(job.id()),
				title,
				trimToNull(job.companyName()),
				trimToNull(job.jobGeo()),
				snippet,
				formatSalary(job),
				formatJobType(job.jobType()),
				trimToNull(job.pubDate()),
				url,
				"Jobicy",
				matchedKeywords
		);
	}

	private boolean passesRelevanceGate(
			String title,
			List<String> matchedKeywords,
			List<String> keywords,
			List<String> roleTokens
	) {
		if (matchedKeywords.isEmpty()) {
			return false;
		}
		String normalizedTitle = normalizeText(title);
		if (keywords.stream().anyMatch(keyword -> keywordMatches(normalizedTitle, keyword))) {
			return true;
		}
		if (roleTokens.isEmpty()) {
			return matchedKeywords.size() >= 2;
		}
		return matchedKeywords.size() >= 2
				&& roleTokens.stream().anyMatch(roleToken -> containsToken(normalizedTitle, roleToken));
	}

	private List<String> significantRoleTokens(String role) {
		String normalizedRole = normalizeForRole(role);
		if (normalizedRole == null) {
			return List.of();
		}
		return Pattern.compile("[^a-z0-9]+")
				.splitAsStream(normalizedRole)
				.filter(token -> !token.isBlank())
				.filter(token -> !GENERIC_ROLE_TOKENS.contains(token))
				.distinct()
				.toList();
	}

	private String normalizeForRole(String value) {
		String withoutAccents = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		return normalizeText(withoutAccents);
	}

	private boolean containsToken(String normalizedText, String token) {
		if (normalizedText == null || token == null || token.isBlank()) {
			return false;
		}
		return Pattern.compile("(^|[^a-z0-9])" + Pattern.quote(token) + "([^a-z0-9]|$)")
				.matcher(normalizedText)
				.find();
	}

	private boolean passesGeoGate(String jobGeo, LocationScope scope) {
		if (scope == LocationScope.GLOBAL) {
			return true;
		}
		String normalizedJobGeo = normalizeForGeo(jobGeo == null ? "" : jobGeo);
		if (normalizedJobGeo.isBlank()) {
			return false;
		}
		if (containsGeoToken(normalizedJobGeo, "anywhere")
				|| containsGeoToken(normalizedJobGeo, "latam")
				|| normalizedJobGeo.contains("latin america")
				|| normalizedJobGeo.contains("america latina")) {
			return true;
		}
		return scope == LocationScope.ARGENTINA && containsGeoToken(normalizedJobGeo, "argentina");
	}

	private boolean containsGeoToken(String normalizedText, String token) {
		return Pattern.compile("(^|[^a-z0-9])" + Pattern.quote(token) + "([^a-z0-9]|$)")
				.matcher(normalizedText)
				.find();
	}

	private boolean isObviousSeniorityMismatch(String title, String jobLevel, JobSeniority seniority) {
		if (seniority != JobSeniority.TRAINEE && seniority != JobSeniority.JUNIOR) {
			return false;
		}
		if (OBVIOUS_SENIORITY_PATTERN.matcher(title).find()) {
			return true;
		}
		String normalizedJobLevel = normalizeText(jobLevel);
		return normalizedJobLevel != null && SENIOR_JOB_LEVEL_PATTERN.matcher(normalizedJobLevel).find();
	}

	private List<String> matchedKeywords(String text, List<String> keywords) {
		String normalizedText = normalizeText(text);
		return keywords.stream()
				.filter(keyword -> keywordMatches(normalizedText, keyword))
				.toList();
	}

	private boolean keywordMatches(String normalizedText, String keyword) {
		String normalizedKeyword = normalizeText(keyword);
		if (normalizedText == null || normalizedKeyword == null) {
			return false;
		}
		if ("rest api".equals(normalizedKeyword)) {
			return Pattern.compile("(?i)(^|[^a-z0-9])(rest\\s+apis?|apis?\\s+rest)([^a-z0-9]|$)")
					.matcher(normalizedText)
					.find();
		}
		String pattern = Pattern.quote(normalizedKeyword).replace("\\ ", "\\E\\\\s+\\Q");
		return Pattern.compile("(?i)(^|[^a-z0-9])" + pattern + "([^a-z0-9]|$)")
				.matcher(normalizedText)
				.find();
	}

	private String normalizeSnippet(String excerpt, String descriptionText) {
		String excerptText = toPlainText(excerpt);
		if (excerptText != null) {
			return excerptText;
		}
		return abbreviate(descriptionText, MAX_DESCRIPTION_SNIPPET_LENGTH);
	}

	private String toPlainText(String value) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			return null;
		}
		String unescaped = HtmlUtils.htmlUnescape(trimmed);
		String withoutTags = UNSAFE_TAG_PATTERN.matcher(unescaped).replaceAll(" ");
		return normalizeWhitespace(withoutTags);
	}

	private String abbreviate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		int codePoints = value.codePointCount(0, value.length());
		int end = value.offsetByCodePoints(0, Math.min(codePoints, maxLength));
		return value.substring(0, end).trim();
	}

	private String safeHttpsUrl(String value) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			return null;
		}
		try {
			URI uri = URI.create(trimmed);
			if (!uri.isAbsolute()
					|| !"https".equalsIgnoreCase(uri.getScheme())
					|| uri.getHost() == null
					|| uri.getHost().isBlank()) {
				return null;
			}
			return uri.toString();
		}
		catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private String formatSalary(JobicyJob job) {
		String min = formatSalaryAmount(job.salaryMin());
		String max = formatSalaryAmount(job.salaryMax());
		String currency = trimToNull(job.salaryCurrency());
		String period = trimToNull(job.salaryPeriod());
		String prefix = currency == null ? "" : currency + " ";
		String salary = null;
		if (min != null && max != null) {
			salary = prefix + min + " - " + max;
		}
		else if (min != null) {
			salary = "Desde " + prefix + min;
		}
		else if (max != null) {
			salary = "Hasta " + prefix + max;
		}
		if (salary == null) {
			return null;
		}
		return period == null ? salary : salary + " " + period;
	}

	private String formatSalaryAmount(Object value) {
		if (value == null) {
			return null;
		}
		String raw = String.valueOf(value).trim();
		if (raw.isBlank()) {
			return null;
		}
		try {
			return new BigDecimal(raw).stripTrailingZeros().toPlainString();
		}
		catch (NumberFormatException exception) {
			return raw;
		}
	}

	private String formatJobType(JsonNode jobType) {
		if (jobType == null || jobType.isNull()) {
			return null;
		}
		List<String> values = new ArrayList<>();
		if (jobType.isArray()) {
			jobType.forEach(item -> {
				if (item.isTextual() && !item.asText().isBlank()) {
					values.add(item.asText().trim());
				}
			});
		}
		else if (jobType.isTextual() && !jobType.asText().isBlank()) {
			values.add(jobType.asText().trim());
		}
		return values.isEmpty() ? null : String.join(", ", values);
	}

	private Instant parseUpdatedAt(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Instant.parse(value);
		}
		catch (DateTimeParseException ignored) {
			try {
				return OffsetDateTime.parse(value).toInstant();
			}
			catch (DateTimeParseException ignoredAgain) {
				try {
					return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
				}
				catch (DateTimeParseException ignoredFinal) {
					return null;
				}
			}
		}
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isBlank() ? null : trimmed;
	}

	private String normalizeText(String value) {
		return normalizeWhitespace(value == null ? "" : value.toLowerCase(Locale.ROOT));
	}

	private String normalizeWhitespace(String value) {
		String normalized = WHITESPACE_PATTERN.matcher(value.trim()).replaceAll(" ");
		return normalized.isBlank() ? null : normalized;
	}

	private record NormalizedJobSearchRequest(
			String role,
			JobSeniority seniority,
			List<String> keywords,
			String location
	) {
	}

	private enum LocationScope {
		ARGENTINA,
		LATAM,
		GLOBAL
	}
}
