package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codercup.jobmatchai.client.JobicyClient;
import com.codercup.jobmatchai.dto.JobSeniority;
import com.codercup.jobmatchai.dto.career.CareerLearningPriority;
import com.codercup.jobmatchai.dto.career.CareerMarketConfidence;
import com.codercup.jobmatchai.dto.career.CareerMarketRequest;
import com.codercup.jobmatchai.dto.career.CareerMarketResponse;
import com.codercup.jobmatchai.dto.career.CareerMultiverseRequest;
import com.codercup.jobmatchai.dto.career.CareerMultiverseResponse;
import com.codercup.jobmatchai.dto.career.CareerPathType;
import com.codercup.jobmatchai.dto.career.CareerRegion;
import com.codercup.jobmatchai.dto.career.CareerSkillDemandResponse;
import com.codercup.jobmatchai.dto.career.internal.CareerGeminiPath;
import com.codercup.jobmatchai.dto.career.internal.CareerGeminiResult;
import com.codercup.jobmatchai.dto.career.internal.CareerProfile;
import com.codercup.jobmatchai.dto.internal.JobicySearchResponse;
import com.codercup.jobmatchai.exception.InvalidCareerAiResponseException;
import com.codercup.jobmatchai.exception.InvalidCareerMultiverseRequestException;
import com.codercup.jobmatchai.exception.JobSearchUnavailableException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CareerMultiverseServiceTest {

	@Test
	void generatesThreeOrderedPathsWithOneGeminiCallOneJobicyCallAndSameSnapshot() {
		Fixture fixture = fixture();
		JobicySearchResponse snapshot = new JobicySearchResponse(List.of());
		when(fixture.jobicyClient.search()).thenReturn(snapshot);
		when(fixture.careerGeminiService.generatePaths(any(CareerProfile.class))).thenReturn(new CareerGeminiResult(List.of(
				path(CareerPathType.ALTERNATIVE, "QA Automation Engineer"),
				path(CareerPathType.NATURAL, "Java Backend Developer"),
				path(CareerPathType.EXPANSION, "Cloud Backend Developer")
		)));
		when(fixture.careerMarketService.analyze(any(CareerMarketRequest.class), same(snapshot)))
				.thenAnswer(invocation -> marketFor(invocation.getArgument(0)));

		CareerMultiverseResponse response = fixture.service.generate(validRequest());

		assertThat(response.provider()).isEqualTo("JOBICY");
		assertThat(response.region()).isEqualTo(CareerRegion.LATAM);
		assertThat(response.profile().skills()).containsExactly("Java", "Spring Boot", "SQL", "REST APIs", "Git");
		assertThat(response.paths()).hasSize(3);
		assertThat(response.paths()).extracting("type").containsExactly(
				CareerPathType.NATURAL,
				CareerPathType.EXPANSION,
				CareerPathType.ALTERNATIVE
		);
		verify(fixture.careerGeminiService, times(1)).generatePaths(any(CareerProfile.class));
		verify(fixture.jobicyClient, times(1)).search();
		verify(fixture.careerMarketService, times(3)).analyze(any(CareerMarketRequest.class), same(snapshot));
	}

	@Test
	void passesRealCurrentSkillsToEachMarketRequestAndCandidateSkillsDoNotIncreaseCoverage() {
		Fixture fixture = fixture();
		JobicySearchResponse snapshot = new JobicySearchResponse(List.of());
		when(fixture.jobicyClient.search()).thenReturn(snapshot);
		when(fixture.careerGeminiService.generatePaths(any())).thenReturn(validGeminiResult());
		when(fixture.careerMarketService.analyze(any(CareerMarketRequest.class), same(snapshot)))
				.thenReturn(market("Java Backend Developer", CareerMarketConfidence.HIGH, 50,
						List.of(new CareerSkillDemandResponse("Docker", 5, 50))));

		CareerMultiverseResponse response = fixture.service.generate(validRequest());

		ArgumentCaptor<CareerMarketRequest> captor = ArgumentCaptor.forClass(CareerMarketRequest.class);
		verify(fixture.careerMarketService, times(3)).analyze(captor.capture(), same(snapshot));
		assertThat(captor.getAllValues()).allSatisfy(request ->
				assertThat(request.currentSkills()).containsExactly("Java", "Spring Boot", "SQL", "REST APIs", "Git"));
		assertThat(captor.getAllValues()).allSatisfy(request ->
				assertThat(request.roleAliases()).containsExactly(request.role() + " Alias"));
		assertThat(response.paths().get(0).market().coveragePercentage()).isEqualTo(50);
		assertThat(response.paths().get(0).market().currentSkillsDetected()).doesNotContain("Docker", "AWS", "Kubernetes");
	}

	@Test
	void preservesMarketResultsAndBuildsLearningPrioritiesDeterministically() {
		Fixture fixture = fixture();
		JobicySearchResponse snapshot = new JobicySearchResponse(List.of());
		when(fixture.jobicyClient.search()).thenReturn(snapshot);
		when(fixture.careerGeminiService.generatePaths(any())).thenReturn(validGeminiResult());
		when(fixture.careerMarketService.analyze(any(CareerMarketRequest.class), same(snapshot))).thenReturn(market(
				"Java Backend Developer",
				CareerMarketConfidence.HIGH,
				0,
				List.of(
						new CareerSkillDemandResponse("Docker", 10, 50),
						new CareerSkillDemandResponse("AWS", 8, 40),
						new CareerSkillDemandResponse("Kafka", 4, 24)
				)
		));

		CareerMultiverseResponse response = fixture.service.generate(validRequest());

		assertThat(response.paths().get(0).market().coveragePercentage()).isZero();
		assertThat(response.paths().get(0).learningPriorities()).extracting("priority")
				.containsExactly(CareerLearningPriority.NOW, CareerLearningPriority.NEXT, CareerLearningPriority.LATER);
		assertThat(response.paths().get(0).learningPriorities()).extracting("skill")
				.containsExactly("Docker", "AWS", "Kafka");
	}

	@Test
	void limitsPrioritiesRoadmapAndChallengeSkills() {
		Fixture fixture = fixture();
		JobicySearchResponse snapshot = new JobicySearchResponse(List.of());
		when(fixture.jobicyClient.search()).thenReturn(snapshot);
		when(fixture.careerGeminiService.generatePaths(any())).thenReturn(validGeminiResult());
		when(fixture.careerMarketService.analyze(any(CareerMarketRequest.class), same(snapshot))).thenReturn(market(
				"Java Backend Developer",
				CareerMarketConfidence.HIGH,
				100,
				List.of(
						new CareerSkillDemandResponse("Docker", 10, 60),
						new CareerSkillDemandResponse("AWS", 9, 55),
						new CareerSkillDemandResponse("Testing", 8, 50),
						new CareerSkillDemandResponse("Kafka", 7, 45),
						new CareerSkillDemandResponse("Redis", 6, 35),
						new CareerSkillDemandResponse("CI/CD", 5, 30),
						new CareerSkillDemandResponse("Jenkins", 4, 20)
				)
		));

		CareerMultiverseResponse response = fixture.service.generate(validRequest());

		assertThat(response.paths().get(0).market().coveragePercentage()).isEqualTo(100);
		assertThat(response.paths().get(0).learningPriorities()).hasSize(6);
		assertThat(response.paths().get(0).roadmap()).hasSizeLessThanOrEqualTo(4);
		assertThat(response.paths().get(0).projectChallenge().skills()).containsExactly("Docker", "AWS", "Testing");
		assertThat(response.paths().get(0).projectChallenge().description())
				.doesNotContain("ya sabes", "ya posees", "experiencia previa");
	}

	@Test
	void insufficientConfidenceClearsGuidanceButPreservesMarketEvidence() {
		Fixture fixture = fixture();
		JobicySearchResponse snapshot = new JobicySearchResponse(List.of());
		when(fixture.jobicyClient.search()).thenReturn(snapshot);
		when(fixture.careerGeminiService.generatePaths(any())).thenReturn(validGeminiResult());
		when(fixture.careerMarketService.analyze(any(CareerMarketRequest.class), same(snapshot))).thenReturn(market(
				"Java Backend Developer",
				CareerMarketConfidence.INSUFFICIENT,
				0,
				List.of(new CareerSkillDemandResponse("Docker", 1, 100))
		));

		CareerMultiverseResponse response = fixture.service.generate(validRequest());

		assertThat(response.paths().get(0).market().confidence()).isEqualTo(CareerMarketConfidence.INSUFFICIENT);
		assertThat(response.paths().get(0).market().skillDemand()).isNotEmpty();
		assertThat(response.paths().get(0).learningPriorities()).isEmpty();
		assertThat(response.paths().get(0).roadmap()).isEmpty();
		assertThat(response.paths().get(0).projectChallenge()).isNull();
	}

	@Test
	void emptyMissingSkillsDoesNotInventGuidance() {
		Fixture fixture = fixture();
		JobicySearchResponse snapshot = new JobicySearchResponse(List.of());
		when(fixture.jobicyClient.search()).thenReturn(snapshot);
		when(fixture.careerGeminiService.generatePaths(any())).thenReturn(validGeminiResult());
		when(fixture.careerMarketService.analyze(any(CareerMarketRequest.class), same(snapshot))).thenReturn(market(
				"Java Backend Developer",
				CareerMarketConfidence.HIGH,
				100,
				List.of()
		));

		CareerMultiverseResponse response = fixture.service.generate(validRequest());

		assertThat(response.paths().get(0).learningPriorities()).isEmpty();
		assertThat(response.paths().get(0).roadmap()).isEmpty();
		assertThat(response.paths().get(0).projectChallenge()).isNull();
	}

	@Test
	void geminiErrorDoesNotCallJobicy() {
		Fixture fixture = fixture();
		when(fixture.careerGeminiService.generatePaths(any())).thenThrow(new InvalidCareerAiResponseException());

		assertThatThrownBy(() -> fixture.service.generate(validRequest()))
				.isInstanceOf(InvalidCareerAiResponseException.class);
		verify(fixture.jobicyClient, never()).search();
	}

	@Test
	void jobicyErrorIsNotReplacedWithFakeMarketData() {
		Fixture fixture = fixture();
		when(fixture.careerGeminiService.generatePaths(any())).thenReturn(validGeminiResult());
		when(fixture.jobicyClient.search()).thenThrow(new JobSearchUnavailableException("unavailable"));

		assertThatThrownBy(() -> fixture.service.generate(validRequest()))
				.isInstanceOf(JobSearchUnavailableException.class);
		verify(fixture.careerMarketService, never()).analyze(any(), any());
	}

	@Test
	void validatesRequestAndDeduplicatesSkillsBeforeGemini() {
		Fixture fixture = fixture();
		JobicySearchResponse snapshot = new JobicySearchResponse(List.of());
		when(fixture.jobicyClient.search()).thenReturn(snapshot);
		when(fixture.careerGeminiService.generatePaths(any())).thenReturn(validGeminiResult());
		when(fixture.careerMarketService.analyze(any(CareerMarketRequest.class), same(snapshot)))
				.thenReturn(market("Java Backend Developer", CareerMarketConfidence.HIGH, 100, List.of()));

		fixture.service.generate(new CareerMultiverseRequest(
				" Java Backend Developer ",
				JobSeniority.JUNIOR,
				List.of("Java", "java", "Postgres", "PostgreSQL"),
				CareerRegion.LATAM
		));

		ArgumentCaptor<CareerProfile> profileCaptor = ArgumentCaptor.forClass(CareerProfile.class);
		verify(fixture.careerGeminiService).generatePaths(profileCaptor.capture());
		assertThat(profileCaptor.getValue().role()).isEqualTo("Java Backend Developer");
		assertThat(profileCaptor.getValue().skills()).containsExactly("Java", "PostgreSQL");
	}

	@Test
	void rejectsInvalidRequests() {
		Fixture fixture = fixture();

		assertThatThrownBy(() -> fixture.service.generate(null)).isInstanceOf(InvalidCareerMultiverseRequestException.class);
		assertThatThrownBy(() -> fixture.service.generate(new CareerMultiverseRequest("", JobSeniority.JUNIOR,
				List.of("Java"), CareerRegion.LATAM))).isInstanceOf(InvalidCareerMultiverseRequestException.class);
		assertThatThrownBy(() -> fixture.service.generate(new CareerMultiverseRequest("A".repeat(81), JobSeniority.JUNIOR,
				List.of("Java"), CareerRegion.LATAM))).isInstanceOf(InvalidCareerMultiverseRequestException.class);
		assertThatThrownBy(() -> fixture.service.generate(new CareerMultiverseRequest("Java", null,
				List.of("Java"), CareerRegion.LATAM))).isInstanceOf(InvalidCareerMultiverseRequestException.class);
		assertThatThrownBy(() -> fixture.service.generate(new CareerMultiverseRequest("Java", JobSeniority.JUNIOR,
				List.of(), CareerRegion.LATAM))).isInstanceOf(InvalidCareerMultiverseRequestException.class);
		assertThatThrownBy(() -> fixture.service.generate(new CareerMultiverseRequest("Java", JobSeniority.JUNIOR,
				List.of("A".repeat(51)), CareerRegion.LATAM))).isInstanceOf(InvalidCareerMultiverseRequestException.class);
		assertThatThrownBy(() -> fixture.service.generate(new CareerMultiverseRequest("Java", JobSeniority.JUNIOR,
				List.of("Java"), null))).isInstanceOf(InvalidCareerMultiverseRequestException.class);
	}

	private Fixture fixture() {
		CareerGeminiService careerGeminiService = mock(CareerGeminiService.class);
		JobicyClient jobicyClient = mock(JobicyClient.class);
		CareerMarketService careerMarketService = mock(CareerMarketService.class);
		return new Fixture(
				careerGeminiService,
				jobicyClient,
				careerMarketService,
				new CareerMultiverseService(
						careerGeminiService,
						jobicyClient,
						careerMarketService,
						new CareerGuidanceService()
				)
		);
	}

	private CareerMultiverseRequest validRequest() {
		return new CareerMultiverseRequest(
				"Java Backend Developer",
				JobSeniority.JUNIOR,
				List.of("Java", "Spring Boot", "SQL", "REST APIs", "Git"),
				CareerRegion.LATAM
		);
	}

	private CareerGeminiResult validGeminiResult() {
		return new CareerGeminiResult(List.of(
				path(CareerPathType.NATURAL, "Java Backend Developer"),
				path(CareerPathType.EXPANSION, "Cloud Backend Developer"),
				path(CareerPathType.ALTERNATIVE, "QA Automation Engineer")
		));
	}

	private CareerGeminiPath path(CareerPathType type, String role) {
		return new CareerGeminiPath(
				type,
				role,
				List.of(role + " Alias"),
				"Summary for " + role,
				"Rationale for " + role,
				List.of("Java", "Spring Boot", "Docker", "AWS")
		);
	}

	private CareerMarketResponse marketFor(CareerMarketRequest request) {
		return market(request.role(), CareerMarketConfidence.LOW, 67,
				List.of(new CareerSkillDemandResponse("Docker", 3, 60)));
	}

	private CareerMarketResponse market(
			String role,
			CareerMarketConfidence confidence,
			int coverage,
			List<CareerSkillDemandResponse> missingSkills
	) {
		List<CareerSkillDemandResponse> demand = new ArrayList<>();
		demand.add(new CareerSkillDemandResponse("Java", 10, 100));
		demand.addAll(missingSkills);
		return new CareerMarketResponse(
				"JOBICY",
				role,
				CareerRegion.LATAM,
				confidence == CareerMarketConfidence.INSUFFICIENT ? 1 : 12,
				confidence,
				coverage,
				List.of("Java", "Spring Boot"),
				missingSkills,
				demand
		);
	}

	private record Fixture(
			CareerGeminiService careerGeminiService,
			JobicyClient jobicyClient,
			CareerMarketService careerMarketService,
			CareerMultiverseService service
	) {
	}
}
