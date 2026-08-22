package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codercup.jobmatchai.client.JobicyClient;
import com.codercup.jobmatchai.dto.JobSeniority;
import com.codercup.jobmatchai.dto.career.CareerMarketConfidence;
import com.codercup.jobmatchai.dto.career.CareerMarketRequest;
import com.codercup.jobmatchai.dto.career.CareerMarketResponse;
import com.codercup.jobmatchai.dto.career.CareerRegion;
import com.codercup.jobmatchai.dto.internal.JobicyJob;
import com.codercup.jobmatchai.dto.internal.JobicySearchResponse;
import com.codercup.jobmatchai.exception.InvalidCareerMarketRequestException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CareerMarketServiceTest {

	@Test
	void javaBackendWithSufficientSampleReturnsMarketDemand() {
		CareerMarketResponse response = serviceWithJobs(List.of(
				job("1", "Java Backend Developer", "Argentina", "Java Spring Boot SQL Docker"),
				job("2", "Backend Engineer", "LATAM", "Java Spring Boot Docker Kubernetes"),
				job("3", "Java Developer", "Anywhere", "Java SQL Testing Docker"),
				job("4", "Java Backend Engineer", "Latin America", "Java REST API Docker"),
				job("5", "Backend Java Developer", "America Latina", "Java Spring Boot PostgreSQL"),
				job("6", "Java API Developer", "LATAM", "Java RESTful API Git"),
				job("7", "Java Backend Developer", "LATAM", "Java Docker Maven"),
				job("8", "Backend Engineer", "Anywhere", "Java Spring Boot JUnit"),
				job("9", "Java Developer", "LATAM", "Java SQL Hibernate"),
				job("10", "Java Backend Developer", "LATAM", "Java Kafka Docker"),
				job("11", "Java Backend Developer", "LATAM", "Java Redis Docker"),
				job("12", "Java Backend Developer", "LATAM", "Java Microservices Docker"),
				job("13", "Backend Engineer", "LATAM", "Java Gradle")
		)).analyze(validRequest());

		assertThat(response.provider()).isEqualTo("JOBICY");
		assertThat(response.role()).isEqualTo("Java Backend Developer");
		assertThat(response.region()).isEqualTo(CareerRegion.LATAM);
		assertThat(response.sampleSize()).isEqualTo(12);
		assertThat(response.confidence()).isEqualTo(CareerMarketConfidence.HIGH);
		assertThat(response.currentSkillsDetected()).contains("Java", "Spring Boot", "SQL", "REST APIs", "Git");
		assertThat(response.missingSkills()).extracting("skill").contains("Docker");
		assertThat(response.skillDemand()).first().extracting("skill").isEqualTo("Java");
	}

	@Test
	void javaAndSqlDoNotMatchJavascriptOrNosql() {
		CareerMarketResponse response = serviceWithJobs(List.of(
				job("1", "Backend Developer", "LATAM", "JavaScript NoSQL"),
				job("2", "Backend Developer", "LATAM", "Java SQL")
		)).analyze(validRequest());

		assertThat(demand(response, "JavaScript")).isEqualTo(1);
		assertThat(demand(response, "SQL")).isEqualTo(1);
		assertThat(demand(response, "Java")).isEqualTo(1);
	}

	@Test
	void aliasesAreCanonicalizedInDemand() {
		CareerMarketResponse response = serviceWithJobs(List.of(
				job("1", "Java Backend Developer", "LATAM", "Postgres K8s"),
				job("2", "Java Backend Developer", "LATAM", "PostgreSQL Kubernetes")
		)).analyze(validRequest());

		assertThat(demand(response, "PostgreSQL")).isEqualTo(2);
		assertThat(demand(response, "Kubernetes")).isEqualTo(2);
	}

	@Test
	void repeatedSkillCountsOnlyOncePerJob() {
		CareerMarketResponse response = serviceWithJobs(List.of(
				job("1", "Java Backend Developer", "LATAM", "Docker Docker Docker Java"),
				job("2", "Java Backend Developer", "LATAM", "Java")
		)).analyze(validRequest());

		assertThat(demand(response, "Docker")).isEqualTo(1);
		assertThat(response.skillDemand()).filteredOn(skill -> skill.skill().equals("Docker"))
				.first()
				.extracting("frequencyPercentage")
				.isEqualTo(50);
	}

	@Test
	void currentSkillsAreDeduplicatedAndCoverageIsWeighted() {
		CareerMarketResponse response = serviceWithJobs(List.of(
				job("1", "Java Backend Developer", "LATAM", "Java Docker"),
				job("2", "Java Backend Developer", "LATAM", "Java Docker"),
				job("3", "Java Backend Developer", "LATAM", "Java SQL")
		)).analyze(new CareerMarketRequest(
				" Java Backend Developer ",
				JobSeniority.JUNIOR,
				List.of("java", "Java", "Postgres"),
				CareerRegion.LATAM
		));

		assertThat(response.role()).isEqualTo("Java Backend Developer");
		assertThat(response.currentSkillsDetected()).containsExactly("Java");
		assertThat(response.coveragePercentage()).isEqualTo(50);
		assertThat(response.coveragePercentage()).isLessThanOrEqualTo(100);
	}

	@Test
	void emptySampleHasInsufficientConfidenceAndZeroCoverage() {
		CareerMarketResponse response = serviceWithJobs(List.of(
				job("1", "Frontend JavaScript Developer", "LATAM", "JavaScript React")
		)).analyze(validRequest());

		assertThat(response.sampleSize()).isZero();
		assertThat(response.confidence()).isEqualTo(CareerMarketConfidence.INSUFFICIENT);
		assertThat(response.coveragePercentage()).isZero();
		assertThat(response.skillDemand()).isEmpty();
	}

	@Test
	void confidenceBoundariesAreExact() {
		assertThat(CareerMarketConfidence.fromSampleSize(0)).isEqualTo(CareerMarketConfidence.INSUFFICIENT);
		assertThat(CareerMarketConfidence.fromSampleSize(1)).isEqualTo(CareerMarketConfidence.INSUFFICIENT);
		assertThat(CareerMarketConfidence.fromSampleSize(2)).isEqualTo(CareerMarketConfidence.LOW);
		assertThat(CareerMarketConfidence.fromSampleSize(4)).isEqualTo(CareerMarketConfidence.LOW);
		assertThat(CareerMarketConfidence.fromSampleSize(5)).isEqualTo(CareerMarketConfidence.MEDIUM);
		assertThat(CareerMarketConfidence.fromSampleSize(11)).isEqualTo(CareerMarketConfidence.MEDIUM);
		assertThat(CareerMarketConfidence.fromSampleSize(12)).isEqualTo(CareerMarketConfidence.HIGH);
	}

	@Test
	void traineeAndJuniorRejectObviousSeniorJobs() {
		List<JobicyJob> jobs = List.of(
				job("1", "Senior Java Backend Developer", "LATAM", "Java"),
				job("2", "Java Backend Developer", "LATAM", "Java", "Lead")
		);

		assertThat(serviceWithJobs(jobs).analyze(validRequest()).sampleSize()).isZero();
		assertThat(serviceWithJobs(jobs).analyze(new CareerMarketRequest(
				"Java Backend Developer", JobSeniority.TRAINEE, List.of("Java"), CareerRegion.LATAM
		)).sampleSize()).isZero();
	}

	@Test
	void regionRulesAreAppliedLocally() {
		assertThat(serviceWithJobs(List.of(job("1", "Java Backend Developer", "Anywhere", "Java")))
				.analyze(validRequest()).sampleSize()).isEqualTo(1);
		assertThat(serviceWithJobs(List.of(job("1", "Java Backend Developer", "LATAM", "Java")))
				.analyze(new CareerMarketRequest("Java Backend Developer", JobSeniority.JUNIOR, List.of("Java"),
						CareerRegion.ARGENTINA)).sampleSize()).isEqualTo(1);
		assertThat(serviceWithJobs(List.of(job("1", "Java Backend Developer", "Europe", "Java")))
				.analyze(new CareerMarketRequest("Java Backend Developer", JobSeniority.JUNIOR, List.of("Java"),
						CareerRegion.GLOBAL)).sampleSize()).isEqualTo(1);
	}

	@Test
	void limitsTopSkillsMissingSkillsAndSortsDeterministically() {
		List<JobicyJob> jobs = new ArrayList<>();
		jobs.add(job("1", "Java Backend Developer", "LATAM",
				"Java Docker SQL AWS Azure GCP Git GitHub GitLab JUnit Mockito Testing CI/CD Jenkins Linux Redis"));
		jobs.add(job("2", "Java Backend Developer", "LATAM",
				"Java Docker SQL AWS Azure GCP Git GitHub GitLab JUnit Mockito Testing CI/CD Jenkins Linux"));

		CareerMarketResponse response = serviceWithJobs(jobs).analyze(new CareerMarketRequest(
				"Java Backend Developer", JobSeniority.JUNIOR, List.of("Java"), CareerRegion.LATAM
		));

		assertThat(response.skillDemand()).hasSize(15);
		assertThat(response.missingSkills()).hasSize(10);
		assertThat(response.skillDemand()).extracting("skill")
				.containsSubsequence("AWS", "Azure", "CI/CD", "Docker", "GCP", "Git");
	}

	@Test
	void htmlDescriptionDoesNotBreakDetection() {
		CareerMarketResponse response = serviceWithJobs(List.of(
				job("1", "Java Backend Developer", "LATAM", "&lt;b&gt;Spring Boot&lt;/b&gt; <i>Docker</i>")
		)).analyze(validRequest());

		assertThat(demand(response, "Spring Boot")).isEqualTo(1);
		assertThat(demand(response, "Docker")).isEqualTo(1);
	}

	@Test
	void calibrationSnapshotKeepsRecallWithoutMatchingFrontendJavascriptOrSeniorRoles() {
		List<JobicyJob> jobs = List.of(
				job("1", "Java Backend Developer", "LATAM", "Java Spring Boot SQL Docker", "Junior"),
				job("2", "Backend Software Engineer", "Anywhere", "Java Spring Boot PostgreSQL REST APIs"),
				job("3", "API Backend Engineer", "LATAM", "REST APIs Java SQL"),
				job("4", "Frontend JavaScript Developer", "LATAM", "JavaScript React CSS"),
				job("5", "Senior Java Developer", "LATAM", "Java Spring Boot", "Senior"),
				job("6", "Python Backend Developer", "Anywhere", "Python FastAPI PostgreSQL Docker")
		);
		CareerMarketService service = serviceWithJobs(jobs);

		assertThat(service.analyze(new CareerMarketRequest(
				"Java Developer",
				JobSeniority.JUNIOR,
				List.of("Java", "Spring Boot", "SQL", "Git"),
				CareerRegion.LATAM
		)).sampleSize()).isEqualTo(3);
		assertThat(service.analyze(new CareerMarketRequest(
				"API Developer",
				JobSeniority.JUNIOR,
				List.of("Java", "Spring Boot", "SQL", "Git"),
				CareerRegion.LATAM
		)).sampleSize()).isEqualTo(1);
		assertThat(service.analyze(new CareerMarketRequest(
				"Python Developer",
				JobSeniority.JUNIOR,
				List.of("Java", "Spring Boot", "SQL", "Python", "Git"),
				CareerRegion.LATAM
		)).sampleSize()).isEqualTo(1);
	}

	@Test
	void roleAliasesImproveApiDeveloperRecallWithoutTreatingAliasesAsCurrentSkills() {
		CareerMarketResponse withoutAlias = serviceWithJobs(List.of(
				job("1", "Backend Software Engineer", "LATAM", "Java Spring Boot PostgreSQL REST APIs"),
				job("2", "Frontend JavaScript Developer", "LATAM", "JavaScript React CSS")
		)).analyze(new CareerMarketRequest(
				"API Developer",
				JobSeniority.JUNIOR,
				List.of("Java", "SQL"),
				CareerRegion.LATAM
		));
		CareerMarketResponse withAlias = serviceWithJobs(List.of(
				job("1", "Backend Software Engineer", "LATAM", "Java Spring Boot PostgreSQL REST APIs"),
				job("2", "Frontend JavaScript Developer", "LATAM", "JavaScript React CSS")
		)).analyze(new CareerMarketRequest(
				"API Developer",
				JobSeniority.JUNIOR,
				List.of("Java", "SQL"),
				CareerRegion.LATAM,
				List.of("Backend Developer", "Backend API Developer")
		));

		assertThat(withoutAlias.sampleSize()).isZero();
		assertThat(withAlias.sampleSize()).isEqualTo(1);
		assertThat(withAlias.currentSkillsDetected()).containsExactly("Java");
		assertThat(withAlias.currentSkillsDetected()).doesNotContain("Spring Boot", "PostgreSQL", "REST APIs");
		assertThat(withAlias.skillDemand()).extracting("skill").doesNotContain("JavaScript", "React", "CSS");
	}

	@Test
	void validatesRequestAndUsesOneLogicalJobicySearch() {
		JobicyClient client = mockClient(List.of());
		CareerMarketService service = new CareerMarketService(client);

		assertThatThrownBy(() -> service.analyze(null)).isInstanceOf(InvalidCareerMarketRequestException.class);
		assertThatThrownBy(() -> service.analyze(new CareerMarketRequest("", JobSeniority.JUNIOR, List.of("Java"),
				CareerRegion.LATAM))).isInstanceOf(InvalidCareerMarketRequestException.class);
		assertThatThrownBy(() -> service.analyze(new CareerMarketRequest("A".repeat(81), JobSeniority.JUNIOR,
				List.of("Java"), CareerRegion.LATAM))).isInstanceOf(InvalidCareerMarketRequestException.class);
		assertThatThrownBy(() -> service.analyze(new CareerMarketRequest("Java", null, List.of("Java"),
				CareerRegion.LATAM))).isInstanceOf(InvalidCareerMarketRequestException.class);
		assertThatThrownBy(() -> service.analyze(new CareerMarketRequest("Java", JobSeniority.JUNIOR, List.of(),
				CareerRegion.LATAM))).isInstanceOf(InvalidCareerMarketRequestException.class);
		assertThatThrownBy(() -> service.analyze(new CareerMarketRequest("Java", JobSeniority.JUNIOR,
				List.of("A".repeat(51)), CareerRegion.LATAM))).isInstanceOf(InvalidCareerMarketRequestException.class);
		assertThatThrownBy(() -> service.analyze(new CareerMarketRequest("Java", JobSeniority.JUNIOR,
				List.of("Java"), null))).isInstanceOf(InvalidCareerMarketRequestException.class);

		service.analyze(validRequest());

		verify(client, times(1)).search();
	}

	@Test
	void analyzeWithProvidedSnapshotDoesNotCallJobicyClient() {
		JobicyClient client = mockClient(List.of());
		CareerMarketService service = new CareerMarketService(client);
		JobicySearchResponse snapshot = new JobicySearchResponse(List.of(
				job("1", "Java Backend Developer", "LATAM", "Java Docker")
		));

		CareerMarketResponse response = service.analyze(validRequest(), snapshot);

		assertThat(response.sampleSize()).isEqualTo(1);
		verify(client, times(0)).search();
	}

	private CareerMarketRequest validRequest() {
		return new CareerMarketRequest(
				"Java Backend Developer",
				JobSeniority.JUNIOR,
				List.of("Java", "Spring Boot", "SQL", "REST APIs", "Git"),
				CareerRegion.LATAM
		);
	}

	private CareerMarketService serviceWithJobs(List<JobicyJob> jobs) {
		return new CareerMarketService(mockClient(jobs));
	}

	private JobicyClient mockClient(List<JobicyJob> jobs) {
		JobicyClient client = mock(JobicyClient.class);
		when(client.search()).thenReturn(new JobicySearchResponse(jobs));
		return client;
	}

	private int demand(CareerMarketResponse response, String skill) {
		return response.skillDemand().stream()
				.filter(demand -> demand.skill().equals(skill))
				.findFirst()
				.map(skillDemand -> skillDemand.jobsMentioning())
				.orElse(0);
	}

	private JobicyJob job(Object id, String title, String location, String description) {
		return job(id, title, location, description, null);
	}

	private JobicyJob job(Object id, String title, String location, String description, String level) {
		return new JobicyJob(
				id,
				"https://example.com/job/" + id,
				title,
				"Example",
				null,
				null,
				location,
				level,
				null,
				description,
				null,
				null,
				null,
				null,
				null
		);
	}
}
