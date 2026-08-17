package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codercup.jobmatchai.client.JobicyClient;
import com.codercup.jobmatchai.dto.JobSearchRequest;
import com.codercup.jobmatchai.dto.JobSearchResponse;
import com.codercup.jobmatchai.dto.JobSeniority;
import com.codercup.jobmatchai.dto.internal.JobicyJob;
import com.codercup.jobmatchai.dto.internal.JobicySearchResponse;
import com.codercup.jobmatchai.exception.InvalidJobSearchRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JobSearchServiceTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void validRequestReturnsNormalizedJobs() {
		JobSearchResponse response = serviceWithJobs(List.of(job(
				"1", "Java Backend Developer", "Example Tech", "Argentina",
				"Java con <b>Spring Boot</b>", "<p>Java con Spring Boot y APIs REST</p>",
				null, jobTypes("full-time", "contract"), "https://example.com/job/1", "2026-08-16T15:30:00Z"
		))).search(validRequest());

		assertThat(response.provider()).isEqualTo("JOBICY");
		assertThat(response.count()).isEqualTo(1);
		assertThat(response.jobs()).hasSize(1);
		assertThat(response.jobs().get(0).source()).isEqualTo("Jobicy");
		assertThat(response.jobs().get(0).snippet()).isEqualTo("Java con Spring Boot");
		assertThat(response.jobs().get(0).employmentType()).isEqualTo("full-time, contract");
		assertThat(response.jobs().get(0).updatedAt()).isEqualTo("2026-08-16T15:30:00Z");
		assertThat(response.jobs().get(0).matchedKeywords()).containsExactly("Java", "Spring Boot", "REST API");
	}

	@Test
	void requestValidationStillApplies() {
		assertThatThrownBy(() -> serviceWithJobs(List.of()).search(new JobSearchRequest(
				" ", JobSeniority.JUNIOR, validKeywords(), "Argentina"
		))).isInstanceOf(InvalidJobSearchRequestException.class);
		assertThatThrownBy(() -> serviceWithJobs(List.of()).search(new JobSearchRequest(
				"Java Backend Developer", JobSeniority.JUNIOR, validKeywords(), " "
		))).isInstanceOf(InvalidJobSearchRequestException.class);
		assertThatThrownBy(() -> serviceWithJobs(List.of()).search(new JobSearchRequest(
				"Java Backend Developer", JobSeniority.JUNIOR, List.of("Java", "SQL"), "Argentina"
		))).isInstanceOf(InvalidJobSearchRequestException.class);
		assertThatThrownBy(() -> serviceWithJobs(List.of()).search(new JobSearchRequest(
				"Java Backend Developer", JobSeniority.JUNIOR,
				List.of("Java", "Spring Boot", "SQL", "MySQL", "REST API", "Git", "Docker"), "Argentina"
		))).isInstanceOf(InvalidJobSearchRequestException.class);
		assertThatThrownBy(() -> serviceWithJobs(List.of()).search(new JobSearchRequest(
				"Java Backend Developer", JobSeniority.JUNIOR,
				List.of("Java", "Spring Boot", "A".repeat(51)), "Argentina"
		))).isInstanceOf(InvalidJobSearchRequestException.class);
		assertThatThrownBy(() -> serviceWithJobs(List.of()).search(new JobSearchRequest(
				"Java Backend Developer", JobSeniority.JUNIOR, List.of("Java", " java ", "SQL"), "Argentina"
		))).isInstanceOf(InvalidJobSearchRequestException.class);
	}

	@Test
	void clientSearchDoesNotReceiveLocationOrScope() {
		JobicyClient client = mockClient(List.of());
		JobSearchService service = new JobSearchService(client, 8);

		service.search(validRequest("Argentina"));
		service.search(validRequest("LATAM"));
		service.search(validRequest("Rosario"));

		verify(client, times(3)).search();
	}

	@Test
	void javaAndSqlDoNotMatchJavascriptOrNosql() {
		JobSearchResponse response = serviceWithJobs(List.of(
				job("1", "JavaScript Developer", null, null, null, "<p>Frontend JavaScript</p>",
						null, null, "https://example.com/job/1", null),
				job("2", "NoSQL Developer", null, null, null, "<p>NoSQL database work</p>",
						null, null, "https://example.com/job/2", null)
		)).search(validRequest());

		assertThat(response.jobs()).isEmpty();
	}

	@Test
	void restApiVariantsMatch() {
		JobSearchResponse response = serviceWithJobs(List.of(job(
				"1", "Java Developer", null, null, null, "<p>APIs REST con Spring Boot</p>",
				null, null, "https://example.com/job/1", null
		))).search(validRequest());

		assertThat(response.jobs().get(0).matchedKeywords()).containsExactly("Java", "Spring Boot", "REST API");
	}

	@Test
	void htmlDescriptionAndEncodedHtmlArePlainText() {
		JobSearchResponse response = serviceWithJobs(List.of(
				job("1", "Backend Engineer", null, null, null, "&lt;b&gt;Java&lt;/b&gt; &lt;i&gt;Spring Boot&lt;/i&gt;",
						null, null, "https://example.com/job/1", null),
				job("2", "Java Developer", null, null, null, "<p>Java</p>",
						null, null, "https://example.com/job/2", null)
		)).search(validRequest());

		assertThat(response.jobs()).extracting("snippet").contains("Java Spring Boot", "Java");
	}

	@Test
	void invalidUrlAndBlankTitleAreDiscarded() {
		JobSearchResponse response = serviceWithJobs(List.of(
				job("1", "Java Developer", null, null, null, "Java", null, null, "http://example.com/job/1", null),
				job("2", " ", null, null, null, "Java", null, null, "https://example.com/job/2", null)
		)).search(validRequest());

		assertThat(response.jobs()).isEmpty();
	}

	@Test
	void juniorFiltersSeniorTitleLeadTitleAndSeniorJobLevel() {
		JobSearchResponse response = serviceWithJobs(List.of(
				job("1", "Senior Java Developer", null, null, null, "Java", null, null, "https://example.com/job/1", null),
				job("2", "Lead Java Developer", null, null, null, "Java", null, null, "https://example.com/job/2", null),
				job("3", "Java Developer", null, null, null, "Java", "Senior", null, "https://example.com/job/3", null),
				job("4", "Java Developer", null, null, null, "Java", "Midweight", null, "https://example.com/job/4", null)
		)).search(validRequest());

		assertThat(response.jobs()).isEmpty();
	}

	@Test
	void juniorAcceptsEntryJuniorAndAnyLevels() {
		JobSearchResponse response = serviceWithJobs(List.of(
				job("1", "Java Developer", null, null, null, "Java", "Entry-Level", null, "https://example.com/job/1", null),
				job("2", "Java Developer", null, null, null, "Java", "Entry-Level, Junior", null, "https://example.com/job/2", null),
				job("3", "Java Developer", null, null, null, "Java", "Any", null, "https://example.com/job/3", null)
		)).search(validRequest());

		assertThat(response.jobs()).hasSize(3);
	}

	@Test
	void midProfileDoesNotUseAggressiveFilter() {
		JobSearchResponse response = serviceWithJobs(List.of(job(
				"1", "Senior Java Developer", null, null, null, "Java", "Senior", null,
				"https://example.com/job/1", null
		))).search(new JobSearchRequest("Java Backend Developer", JobSeniority.MID, validKeywords(), "Argentina"));

		assertThat(response.jobs()).hasSize(1);
	}

	@Test
	void relevanceGateDiscardsIrrelevantAndWeakDescriptionOnlyMatches() {
		JobSearchResponse response = serviceWithJobs(List.of(
				job("1", "People Analytics", null, null, null, "<p>SQL</p>", null, null, "https://example.com/job/1", null),
				job("2", "Java Developer", null, null, null, "<p>Java</p>", null, null, "https://example.com/job/2", null),
				job("3", "Software Engineer", null, null, null, "<p>Java and Spring Boot</p>", null, null,
						"https://example.com/job/3", null),
				job("4", "Engineering Manager", null, null, null, "<p>Docker</p>", null, null,
						"https://example.com/job/4", null)
		)).search(validRequest());

		assertThat(response.jobs()).extracting("id").containsExactly("2");
	}

	@Test
	void relevanceGateRequiresRoleTokenWhenOnlyDescriptionMatchesMultipleKeywords() {
		JobSearchResponse response = serviceWithJobs(List.of(
				job("1", "Senior Fullstack Engineer (.NET/React)", null, null, null,
						"SQL REST API Git", null, null, "https://example.com/job/1", null),
				job("2", "Principal Full-Stack Engineer (.NET/Angular)", null, null, null,
						"SQL REST API Git", null, null, "https://example.com/job/2", null),
				job("3", "Full Stack Developer - Java & React", null, null, null,
						"Java Spring Boot", null, null, "https://example.com/job/3", null),
				job("4", "Backend Engineer", null, null, null,
						"Java Spring Boot SQL", null, null, "https://example.com/job/4", null)
		)).search(new JobSearchRequest(
				"Java Backend Developer", JobSeniority.UNSPECIFIED,
				List.of("Java", "Spring Boot", "SQL", "MySQL", "REST API", "Git"),
				"Argentina"
		));

		assertThat(response.jobs()).extracting("id").containsExactly("4", "3");
	}

	@Test
	void argentinaRequestFiltersJobGeoLocally() {
		JobSearchResponse response = serviceWithJobs(List.of(
				job("1", "Java Developer", null, "Argentina", null, "Java", null, null, "https://example.com/job/1", null),
				job("2", "Java Developer", null, "LATAM", null, "Java", null, null, "https://example.com/job/2", null),
				job("3", "Java Developer", null, "Anywhere", null, "Java", null, null, "https://example.com/job/3", null),
				job("4", "Java Developer", null, "EMEA, LATAM, Canada, USA", null, "Java", null, null,
						"https://example.com/job/4", null),
				job("5", "Java Developer", null, "USA", null, "Java", null, null, "https://example.com/job/5", null),
				job("6", "Java Developer", null, "Europe", null, "Java", null, null, "https://example.com/job/6", null)
		)).search(validRequest("AR"));

		assertThat(response.jobs()).extracting("id").containsExactly("1", "2", "3", "4");
	}

	@Test
	void latamRequestFiltersJobGeoLocally() {
		JobSearchResponse response = serviceWithJobs(List.of(
				job("1", "Java Developer", null, "LATAM", null, "Java", null, null, "https://example.com/job/1", null),
				job("2", "Java Developer", null, "Latin America", null, "Java", null, null, "https://example.com/job/2", null),
				job("3", "Java Developer", null, "Anywhere", null, "Java", null, null, "https://example.com/job/3", null),
				job("4", "Java Developer", null, "USA", null, "Java", null, null, "https://example.com/job/4", null)
		)).search(validRequest("América Latina"));

		assertThat(response.jobs()).extracting("id").containsExactly("1", "2", "3");
	}

	@Test
	void unknownLocationDoesNotFilterJobGeo() {
		JobSearchResponse response = serviceWithJobs(List.of(
				job("1", "Java Developer", null, "USA", null, "Java", null, null, "https://example.com/job/1", null),
				job("2", "Java Developer", null, "Europe", null, "Java", null, null, "https://example.com/job/2", null)
		)).search(validRequest("Rosario"));

		assertThat(response.jobs()).extracting("id").containsExactly("1", "2");
	}

	@Test
	void rankingUsesMatchedKeywordCountThenDateDescendingAndIgnoresInvalidDates() {
		JobSearchResponse response = serviceWithJobs(List.of(
				job("1", "Java Developer", null, null, null, "<p>Java</p>", null, null,
						"https://example.com/job/1", "2026-08-15T00:00:00Z"),
				job("2", "Java Developer", null, null, null, "<p>Java</p>", null, null,
						"https://example.com/job/2", "2026-08-16T00:00:00Z"),
				job("3", "Backend Engineer", null, null, null, "<p>Java Spring Boot SQL</p>", null, null,
						"https://example.com/job/3", "not-a-date")
		)).search(validRequest());

		assertThat(response.jobs()).extracting("id").containsExactly("3", "2", "1");
	}

	@Test
	void limitsReturnedJobsToEightAndCountMatchesReturnedJobs() {
		List<JobicyJob> jobs = new ArrayList<>();
		for (int index = 1; index <= 12; index++) {
			jobs.add(job(String.valueOf(index), "Java Developer " + index, null, null, null,
					"Java Spring Boot SQL", null, null, "https://example.com/job/" + index, null));
		}

		JobSearchResponse response = serviceWithJobs(jobs).search(validRequest());

		assertThat(response.jobs()).hasSize(8);
		assertThat(response.count()).isEqualTo(8);
	}

	@Test
	void salaryIsFormattedWhenPresent() {
		JobSearchResponse response = serviceWithJobs(List.of(new JobicyJob(
				"1", "https://example.com/job/1", "Java Developer", "Example", null, null, "Argentina", null,
				null, "Java", "2026-08-16T00:00:00Z", 100000, 140000, "USD", "yearly"
		))).search(validRequest());

		assertThat(response.jobs().get(0).salary()).isEqualTo("USD 100000 - 140000 yearly");
	}

	@Test
	void responseDoesNotExposeMatchPercentage() {
		assertThat(JobSearchResponse.class.getRecordComponents())
				.extracting(java.lang.reflect.RecordComponent::getName)
				.doesNotContain("matchPercentage");
	}

	private JobSearchRequest validRequest() {
		return validRequest("Argentina");
	}

	private JobSearchRequest validRequest(String location) {
		return new JobSearchRequest("Java Backend Developer", JobSeniority.JUNIOR, validKeywords(), location);
	}

	private List<String> validKeywords() {
		return List.of("Java", "Spring Boot", "SQL", "REST API");
	}

	private JobSearchService serviceWithJobs(List<JobicyJob> jobs) {
		return new JobSearchService(mockClient(jobs), 8);
	}

	private JobicyClient mockClient(List<JobicyJob> jobs) {
		JobicyClient client = mock(JobicyClient.class);
		when(client.search()).thenReturn(new JobicySearchResponse(jobs));
		return client;
	}

	private JobicyJob job(
			Object id,
			String title,
			String company,
			String location,
			String excerpt,
			String description,
			String level,
			JsonNode type,
			String url,
			String pubDate
	) {
		String resolvedLocation = location == null ? "Anywhere" : location;
		return new JobicyJob(id, url, title, company, jobTypes("Software Engineering"), type, resolvedLocation, level, excerpt, description,
				pubDate, null, null, null, null);
	}

	private JsonNode jobTypes(String... values) {
		return objectMapper.valueToTree(List.of(values));
	}
}
