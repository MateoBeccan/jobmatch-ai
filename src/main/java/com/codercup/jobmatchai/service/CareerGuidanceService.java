package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.dto.career.CareerLearningPriority;
import com.codercup.jobmatchai.dto.career.CareerLearningPriorityResponse;
import com.codercup.jobmatchai.dto.career.CareerMarketConfidence;
import com.codercup.jobmatchai.dto.career.CareerMarketResponse;
import com.codercup.jobmatchai.dto.career.CareerProjectChallengeResponse;
import com.codercup.jobmatchai.dto.career.CareerRoadmapStepResponse;
import com.codercup.jobmatchai.dto.career.CareerSkillDemandResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CareerGuidanceService {

	private static final int MAX_PRIORITIES = 6;
	private static final int MAX_ROADMAP_STEPS = 4;
	private static final int MAX_CHALLENGE_SKILLS = 3;

	public List<CareerLearningPriorityResponse> learningPriorities(CareerMarketResponse market) {
		if (market.confidence() == CareerMarketConfidence.INSUFFICIENT || market.missingSkills().isEmpty()) {
			return List.of();
		}
		return market.missingSkills().stream()
				.sorted(Comparator
						.comparingInt(CareerSkillDemandResponse::frequencyPercentage).reversed()
						.thenComparing(CareerSkillDemandResponse::skill))
				.limit(MAX_PRIORITIES)
				.map(skill -> new CareerLearningPriorityResponse(
						skill.skill(),
						skill.jobsMentioning(),
						skill.frequencyPercentage(),
						priorityFor(skill.frequencyPercentage())
				))
				.toList();
	}

	public List<CareerRoadmapStepResponse> roadmap(String role, List<CareerLearningPriorityResponse> priorities) {
		if (priorities.isEmpty()) {
			return List.of();
		}
		List<CareerRoadmapStepResponse> skillSteps = priorities.stream()
				.limit(MAX_ROADMAP_STEPS - 1L)
				.map(priority -> new CareerRoadmapStepResponse(
						0,
						"Aprende fundamentos de " + priority.skill(),
						"Practica " + priority.skill() + " con ejercicios alineados a " + role + "."
				))
				.toList();
		java.util.ArrayList<CareerRoadmapStepResponse> roadmap = new java.util.ArrayList<>();
		for (int index = 0; index < skillSteps.size(); index++) {
			CareerRoadmapStepResponse step = skillSteps.get(index);
			roadmap.add(new CareerRoadmapStepResponse(index + 1, step.title(), step.description()));
		}
		if (roadmap.size() < MAX_ROADMAP_STEPS) {
			roadmap.add(new CareerRoadmapStepResponse(
					roadmap.size() + 1,
					"Documenta y publica el proyecto",
					"Resume las decisiones tecnicas y deja evidencia revisable de tu avance para " + role + "."
			));
		}
		return List.copyOf(roadmap);
	}

	public CareerProjectChallengeResponse projectChallenge(
			String role,
			List<CareerLearningPriorityResponse> priorities
	) {
		if (priorities.isEmpty()) {
			return null;
		}
		List<String> skills = priorities.stream()
				.limit(MAX_CHALLENGE_SKILLS)
				.map(CareerLearningPriorityResponse::skill)
				.toList();
		return new CareerProjectChallengeResponse(
				"Prepara un proyecto de " + role + " listo para mostrar",
				"Crea o adapta una aplicacion relacionada con " + role + " e incorpora "
						+ String.join(", ", skills)
						+ " para convertir estas habilidades en evidencia de portfolio.",
				skills
		);
	}

	private CareerLearningPriority priorityFor(int frequencyPercentage) {
		if (frequencyPercentage >= 50) {
			return CareerLearningPriority.NOW;
		}
		if (frequencyPercentage >= 25) {
			return CareerLearningPriority.NEXT;
		}
		return CareerLearningPriority.LATER;
	}
}
