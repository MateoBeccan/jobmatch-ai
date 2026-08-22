package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.client.JobicyClient;
import com.codercup.jobmatchai.dto.JobSeniority;
import com.codercup.jobmatchai.dto.career.CareerMarketConfidence;
import com.codercup.jobmatchai.dto.career.CareerMarketRequest;
import com.codercup.jobmatchai.dto.career.CareerMarketResponse;
import com.codercup.jobmatchai.dto.career.CareerRegion;
import com.codercup.jobmatchai.dto.career.CareerSkillDemandResponse;
import com.codercup.jobmatchai.dto.internal.JobicyJob;
import com.codercup.jobmatchai.dto.internal.JobicySearchResponse;
import com.codercup.jobmatchai.exception.InvalidCareerMarketRequestException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class CareerMarketService {

	private static final int MAX_ROLE_LENGTH = 80;
	private static final int MAX_ROLE_ALIASES = 4;
	private static final int MAX_CURRENT_SKILLS = 20;
	private static final int MAX_CURRENT_SKILL_LENGTH = 50;
	private static final int MAX_SKILL_DEMAND = 15;
	private static final int MAX_MISSING_SKILLS = 10;
	private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
	private static final Pattern SENIOR_TITLE_PATTERN = Pattern.compile(
			"(?i)(^|[^a-z0-9])(senior|sr\\.?|lead|staff|principal|director|manager|executive)([^a-z0-9]|$)"
	);
	private static final Set<String> GENERIC_ROLE_TOKENS = Set.of(
			"developer", "developers", "engineer", "engineers", "software", "programmer", "programmers",
			"specialist", "specialists"
	);

	private final JobicyClient jobicyClient;

	public CareerMarketService(JobicyClient jobicyClient) {
		this.jobicyClient = jobicyClient;
	}

	public CareerMarketResponse analyze(CareerMarketRequest request) {
		NormalizedCareerMarketRequest normalized = validateAndNormalize(request);
		JobicySearchResponse providerResponse = jobicyClient.search();
		return analyze(normalized, providerResponse);
	}

	public CareerMarketResponse analyze(CareerMarketRequest request, JobicySearchResponse marketSnapshot) {
		NormalizedCareerMarketRequest normalized = validateAndNormalize(request);
		return analyze(normalized, marketSnapshot);
	}

	private CareerMarketResponse analyze(NormalizedCareerMarketRequest normalized, JobicySearchResponse providerResponse) {
		List<JobicyJob> relatedJobs = relatedJobs(providerResponse.jobs(), normalized);
		List<CareerSkillDemandResponse> fullDemand = skillDemand(relatedJobs);
		List<CareerSkillDemandResponse> topDemand = fullDemand.stream()
				.limit(MAX_SKILL_DEMAND)
				.toList();
		Set<String> currentSkillKeys = normalized.currentSkills().stream()
				.map(SkillNormalizer::comparisonKey)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		List<String> currentSkillsDetected = normalized.currentSkills().stream()
				.filter(skill -> fullDemand.stream()
						.anyMatch(demand -> SkillNormalizer.equivalentSkill(skill, demand.skill())))
				.toList();
		List<CareerSkillDemandResponse> missingSkills = topDemand.stream()
				.filter(demand -> !currentSkillKeys.contains(SkillNormalizer.comparisonKey(demand.skill())))
				.limit(MAX_MISSING_SKILLS)
				.toList();

		return new CareerMarketResponse(
				"JOBICY",
				normalized.role(),
				normalized.region(),
				relatedJobs.size(),
				CareerMarketConfidence.fromSampleSize(relatedJobs.size()),
				coveragePercentage(topDemand, currentSkillKeys),
				currentSkillsDetected,
				missingSkills,
				topDemand
		);
	}

	private NormalizedCareerMarketRequest validateAndNormalize(CareerMarketRequest request) {
		if (request == null || request.role() == null || request.seniority() == null
				|| request.currentSkills() == null || request.region() == null) {
			throw new InvalidCareerMarketRequestException();
		}
		String role = request.role().trim();
		if (role.isBlank() || role.length() > MAX_ROLE_LENGTH
				|| request.currentSkills().isEmpty() || request.currentSkills().size() > MAX_CURRENT_SKILLS) {
			throw new InvalidCareerMarketRequestException();
		}
		for (String skill : request.currentSkills()) {
			if (skill == null || skill.trim().isBlank() || skill.trim().length() > MAX_CURRENT_SKILL_LENGTH) {
				throw new InvalidCareerMarketRequestException();
			}
		}
		List<String> currentSkills = SkillNormalizer.normalizeSkillList(request.currentSkills());
		List<String> roleAliases = normalizeRoleAliases(request.roleAliases());
		return new NormalizedCareerMarketRequest(role, request.seniority(), currentSkills, request.region(), roleAliases);
	}

	private List<JobicyJob> relatedJobs(List<JobicyJob> jobs, NormalizedCareerMarketRequest request) {
		if (jobs == null || jobs.isEmpty()) {
			return List.of();
		}
		List<String> roleTokens = significantRoleTokens(request.role(), request.roleAliases());
		return jobs.stream()
				.filter(job -> job != null)
				.filter(job -> trimToNull(job.jobTitle()) != null)
				.filter(job -> passesGeoGate(job.jobGeo(), request.region()))
				.filter(job -> !isObviousSeniorityMismatch(job.jobTitle(), job.jobLevel(), request.seniority()))
				.filter(job -> passesRelevanceGate(job, roleTokens))
				.toList();
	}

	private boolean passesRelevanceGate(JobicyJob job, List<String> roleTokens) {
		String title = plainText(job.jobTitle());
		String body = plainText(String.join(" ", List.of(
				job.jobDescription() == null ? "" : job.jobDescription(),
				job.jobExcerpt() == null ? "" : job.jobExcerpt()
		)));
		String fullText = title + " " + body;
		List<String> mentionedSkills = SkillNormalizer.canonicalSkills().stream()
				.filter(skill -> SkillNormalizer.containsCanonicalSkill(fullText, skill))
				.toList();
		if (mentionedSkills.isEmpty()) {
			return false;
		}
		if (roleTokens.stream().anyMatch(token -> containsToken(normalizeText(title), token))) {
			return true;
		}
		if (roleTokens.isEmpty()
				&& mentionedSkills.stream().anyMatch(skill -> SkillNormalizer.containsCanonicalSkill(title, skill))) {
			return true;
		}
		return mentionedSkills.size() >= 2
				&& roleTokens.stream().anyMatch(token -> containsToken(normalizeText(fullText), token));
	}

	private List<CareerSkillDemandResponse> skillDemand(List<JobicyJob> jobs) {
		if (jobs.isEmpty()) {
			return List.of();
		}
		Map<String, Long> counts = SkillNormalizer.canonicalSkills().stream()
				.collect(Collectors.toMap(
						Function.identity(),
						skill -> jobs.stream()
								.filter(job -> SkillNormalizer.containsCanonicalSkill(jobMarketText(job), skill))
								.count()
				));
		return counts.entrySet().stream()
				.filter(entry -> entry.getValue() > 0)
				.map(entry -> new CareerSkillDemandResponse(
						entry.getKey(),
						Math.toIntExact(entry.getValue()),
						percentage(entry.getValue(), jobs.size())
				))
				.sorted(Comparator
						.comparingInt(CareerSkillDemandResponse::jobsMentioning).reversed()
						.thenComparing(CareerSkillDemandResponse::skill))
				.toList();
	}

	// Weighted coverage of observed market skills; not a hiring probability.
	private int coveragePercentage(List<CareerSkillDemandResponse> skillDemand, Set<String> currentSkillKeys) {
		int totalWeight = skillDemand.stream().mapToInt(CareerSkillDemandResponse::jobsMentioning).sum();
		if (totalWeight == 0) {
			return 0;
		}
		int coveredWeight = skillDemand.stream()
				.filter(demand -> currentSkillKeys.contains(SkillNormalizer.comparisonKey(demand.skill())))
				.mapToInt(CareerSkillDemandResponse::jobsMentioning)
				.sum();
		return Math.min(100, percentage(coveredWeight, totalWeight));
	}

	private int percentage(long numerator, int denominator) {
		if (denominator <= 0) {
			return 0;
		}
		return Math.max(0, Math.min(100, (int) Math.round(numerator * 100.0 / denominator)));
	}

	private String jobMarketText(JobicyJob job) {
		return String.join(" ", List.of(
				job.jobTitle() == null ? "" : job.jobTitle(),
				job.jobDescription() == null ? "" : job.jobDescription(),
				job.jobExcerpt() == null ? "" : job.jobExcerpt()
		));
	}

	private boolean passesGeoGate(String jobGeo, CareerRegion region) {
		if (region == CareerRegion.GLOBAL) {
			return true;
		}
		String normalizedJobGeo = normalizeForGeo(jobGeo == null ? "" : jobGeo);
		if (normalizedJobGeo.isBlank()) {
			return false;
		}
		if (containsToken(normalizedJobGeo, "anywhere")
				|| containsToken(normalizedJobGeo, "latam")
				|| normalizedJobGeo.contains("latin america")
				|| normalizedJobGeo.contains("america latina")) {
			return true;
		}
		return region == CareerRegion.ARGENTINA && containsToken(normalizedJobGeo, "argentina");
	}

	private boolean isObviousSeniorityMismatch(String title, String jobLevel, JobSeniority seniority) {
		if (seniority != JobSeniority.TRAINEE && seniority != JobSeniority.JUNIOR) {
			return false;
		}
		return SENIOR_TITLE_PATTERN.matcher(normalizeText(title)).find()
				|| SENIOR_TITLE_PATTERN.matcher(normalizeText(jobLevel)).find();
	}

	private List<String> normalizeRoleAliases(List<String> aliases) {
		if (aliases == null || aliases.isEmpty()) {
			return List.of();
		}
		if (aliases.size() > MAX_ROLE_ALIASES) {
			throw new InvalidCareerMarketRequestException();
		}
		List<String> normalized = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		for (String alias : aliases) {
			if (alias == null) {
				throw new InvalidCareerMarketRequestException();
			}
			String trimmed = alias.trim();
			if (trimmed.isBlank() || trimmed.length() > MAX_ROLE_LENGTH) {
				throw new InvalidCareerMarketRequestException();
			}
			String key = normalizeForGeo(trimmed);
			if (seen.add(key)) {
				normalized.add(trimmed);
			}
		}
		return List.copyOf(normalized);
	}

	private List<String> significantRoleTokens(String role, List<String> roleAliases) {
		List<String> tokens = new ArrayList<>();
		List<String> roles = new ArrayList<>();
		roles.add(role);
		roles.addAll(roleAliases);
		for (String value : roles) {
			String normalizedRole = normalizeForGeo(value);
			for (String token : Pattern.compile("[^a-z0-9]+").split(normalizedRole)) {
				if (!token.isBlank() && !GENERIC_ROLE_TOKENS.contains(token) && !tokens.contains(token)) {
					tokens.add(token);
				}
			}
		}
		return List.copyOf(tokens);
	}

	private boolean containsToken(String normalizedText, String token) {
		if (normalizedText == null || normalizedText.isBlank() || token == null || token.isBlank()) {
			return false;
		}
		return Pattern.compile("(^|[^a-z0-9])" + Pattern.quote(token) + "([^a-z0-9]|$)")
				.matcher(normalizedText)
				.find();
	}

	private String plainText(String value) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			return "";
		}
		String unescaped = HtmlUtils.htmlUnescape(trimmed);
		return normalizeWhitespace(TAG_PATTERN.matcher(unescaped).replaceAll(" "));
	}

	private String normalizeForGeo(String value) {
		String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT);
		return normalizeWhitespace(normalized);
	}

	private String normalizeText(String value) {
		return normalizeWhitespace(value == null ? "" : value.toLowerCase(Locale.ROOT));
	}

	private String normalizeWhitespace(String value) {
		return WHITESPACE_PATTERN.matcher(value.trim()).replaceAll(" ");
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isBlank() ? null : trimmed;
	}

	private record NormalizedCareerMarketRequest(
			String role,
			JobSeniority seniority,
			List<String> currentSkills,
			CareerRegion region,
			List<String> roleAliases
	) {
	}
}
