package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.client.JobicyClient;
import com.codercup.jobmatchai.dto.career.CareerMarketRequest;
import com.codercup.jobmatchai.dto.career.CareerMarketResponse;
import com.codercup.jobmatchai.dto.career.CareerMultiverseRequest;
import com.codercup.jobmatchai.dto.career.CareerMultiverseResponse;
import com.codercup.jobmatchai.dto.career.CareerPathMarketResponse;
import com.codercup.jobmatchai.dto.career.CareerPathResponse;
import com.codercup.jobmatchai.dto.career.CareerPathType;
import com.codercup.jobmatchai.dto.career.CareerProfileResponse;
import com.codercup.jobmatchai.dto.career.internal.CareerGeminiPath;
import com.codercup.jobmatchai.dto.career.internal.CareerGeminiResult;
import com.codercup.jobmatchai.dto.career.internal.CareerProfile;
import com.codercup.jobmatchai.dto.internal.JobicySearchResponse;
import com.codercup.jobmatchai.exception.InvalidCareerMultiverseRequestException;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class CareerMultiverseService {

	private static final int MAX_ROLE_LENGTH = 80;
	private static final int MAX_SKILLS = 20;
	private static final int MAX_SKILL_LENGTH = 50;

	private final CareerGeminiService careerGeminiService;
	private final JobicyClient jobicyClient;
	private final CareerMarketService careerMarketService;
	private final CareerGuidanceService careerGuidanceService;

	public CareerMultiverseService(
			CareerGeminiService careerGeminiService,
			JobicyClient jobicyClient,
			CareerMarketService careerMarketService,
			CareerGuidanceService careerGuidanceService
	) {
		this.careerGeminiService = careerGeminiService;
		this.jobicyClient = jobicyClient;
		this.careerMarketService = careerMarketService;
		this.careerGuidanceService = careerGuidanceService;
	}

	public CareerMultiverseResponse generate(CareerMultiverseRequest request) {
		NormalizedCareerMultiverseRequest normalized = validateAndNormalize(request);
		CareerProfile profile = new CareerProfile(normalized.role(), normalized.seniority(), normalized.skills());
		CareerGeminiResult geminiResult = careerGeminiService.generatePaths(profile);
		JobicySearchResponse marketSnapshot = jobicyClient.search();

		List<CareerPathResponse> paths = orderedPaths(geminiResult).stream()
				.map(path -> buildPathResponse(path, normalized, marketSnapshot))
				.toList();

		return new CareerMultiverseResponse(
				"JOBICY",
				normalized.region(),
				new CareerProfileResponse(normalized.role(), normalized.seniority(), normalized.skills()),
				paths
		);
	}

	private CareerPathResponse buildPathResponse(
			CareerGeminiPath path,
			NormalizedCareerMultiverseRequest profile,
			JobicySearchResponse marketSnapshot
	) {
		CareerMarketResponse market = careerMarketService.analyze(new CareerMarketRequest(
				path.role(),
				profile.seniority(),
				profile.skills(),
				profile.region(),
				path.aliases()
		), marketSnapshot);
		List<com.codercup.jobmatchai.dto.career.CareerLearningPriorityResponse> priorities =
				careerGuidanceService.learningPriorities(market);
		return new CareerPathResponse(
				path.type(),
				path.role(),
				path.summary(),
				path.rationale(),
				new CareerPathMarketResponse(
						market.sampleSize(),
						market.confidence(),
						market.coveragePercentage(),
						market.currentSkillsDetected(),
						market.missingSkills(),
						market.skillDemand()
				),
				priorities,
				careerGuidanceService.roadmap(path.role(), priorities),
				careerGuidanceService.projectChallenge(path.role(), priorities)
		);
	}

	private List<CareerGeminiPath> orderedPaths(CareerGeminiResult result) {
		Map<CareerPathType, CareerGeminiPath> byType = new EnumMap<>(CareerPathType.class);
		for (CareerGeminiPath path : result.paths()) {
			byType.put(path.type(), path);
		}
		return List.of(
				byType.get(CareerPathType.NATURAL),
				byType.get(CareerPathType.EXPANSION),
				byType.get(CareerPathType.ALTERNATIVE)
		);
	}

	private NormalizedCareerMultiverseRequest validateAndNormalize(CareerMultiverseRequest request) {
		if (request == null || request.role() == null || request.seniority() == null
				|| request.skills() == null || request.region() == null) {
			throw new InvalidCareerMultiverseRequestException();
		}
		String role = request.role().trim();
		if (role.isBlank() || role.length() > MAX_ROLE_LENGTH
				|| request.skills().isEmpty() || request.skills().size() > MAX_SKILLS) {
			throw new InvalidCareerMultiverseRequestException();
		}
		for (String skill : request.skills()) {
			if (skill == null || skill.trim().isBlank() || skill.trim().length() > MAX_SKILL_LENGTH) {
				throw new InvalidCareerMultiverseRequestException();
			}
		}
		List<String> skills = SkillNormalizer.normalizeSkillList(request.skills());
		if (skills.isEmpty()) {
			throw new InvalidCareerMultiverseRequestException();
		}
		return new NormalizedCareerMultiverseRequest(role, request.seniority(), skills, request.region());
	}

	private record NormalizedCareerMultiverseRequest(
			String role,
			com.codercup.jobmatchai.dto.JobSeniority seniority,
			List<String> skills,
			com.codercup.jobmatchai.dto.career.CareerRegion region
	) {
	}
}
