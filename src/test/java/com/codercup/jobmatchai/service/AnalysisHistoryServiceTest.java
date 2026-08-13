package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codercup.jobmatchai.entity.AnalysisEntity;
import com.codercup.jobmatchai.repository.AnalysisRepository;
import com.codercup.jobmatchai.security.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AnalysisHistoryServiceTest {

	@Mock
	private AnalysisRepository repository;

	@Mock
	private CurrentUserService currentUserService;

	private AnalysisHistoryService service;

	@BeforeEach
	void setUp() {
		service = new AnalysisHistoryService(repository, new ObjectMapper(), currentUserService, 50);
		when(currentUserService.currentUserId()).thenReturn("demo");
	}

	@Test
	void findPageOnlyQueriesTheAuthenticatedOwner() {
		AnalysisEntity entity = new AnalysisEntity(
				"demo", "cv.pdf", "v1", "Backend", "Empresa", "Java", "text", 80,
				Instant.parse("2026-01-01T00:00:00Z"), "{\"matchPercentage\":80,\"matchingSkills\":[],\"missingSkills\":[],\"recommendations\":[],\"interviewQuestions\":[]}"
		);
		when(repository.findAllByOwnerId(eq("demo"), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(entity)));

		var page = service.findPage(0, 20);

		assertThat(page.content()).hasSize(1);
		assertThat(page.content().getFirst().role()).isEqualTo("Backend");
		verify(repository).findAllByOwnerId(eq("demo"), any(Pageable.class));
	}

	@Test
	void deletingAnUnknownOwnerRecordReturnsNotFound() {
		when(repository.deleteByIdAndOwnerId("id", "demo")).thenReturn(0L);

		assertThatThrownBy(() -> service.deleteById("id"))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("El análisis no existe");
	}
}
