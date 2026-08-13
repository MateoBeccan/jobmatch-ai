package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.dto.AnalysisHistoryPageResponse;
import com.codercup.jobmatchai.dto.AnalysisHistoryResponse;
import com.codercup.jobmatchai.dto.AnalysisResponse;
import com.codercup.jobmatchai.dto.AnalysisSummaryResponse;
import com.codercup.jobmatchai.entity.AnalysisEntity;
import com.codercup.jobmatchai.repository.AnalysisRepository;
import com.codercup.jobmatchai.security.CurrentUserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AnalysisHistoryService {

	private final AnalysisRepository repository;
	private final ObjectMapper objectMapper;
	private final CurrentUserService currentUserService;
	private final int maxPageSize;

	public AnalysisHistoryService(
			AnalysisRepository repository,
			ObjectMapper objectMapper,
			CurrentUserService currentUserService,
			@org.springframework.beans.factory.annotation.Value("${analysis.history-max-page-size:50}") int maxPageSize
	) {
		this.repository = repository;
		this.objectMapper = objectMapper;
		this.currentUserService = currentUserService;
		if (maxPageSize < 1) {
			throw new IllegalArgumentException("El tamaño máximo de página debe ser mayor a 0.");
		}
		this.maxPageSize = maxPageSize;
	}

	@Transactional
	public AnalysisHistoryResponse save(String cvFileName, String cvVersion, String role, String company,
			String jobDescription, String mode, AnalysisResponse result) {
		try {
			AnalysisEntity entity = new AnalysisEntity(
					currentUserService.currentUserId(),
					limit(cvFileName, 160, "El nombre del CV es demasiado largo."),
					limit(cvVersion, 120, "La versión del CV es demasiado larga."),
					limit(role, 160, "El rol de la oferta es demasiado largo."),
					limit(company, 120, "La empresa de la oferta es demasiado larga."),
					jobDescription,
					mode,
					result.matchPercentage(),
					Instant.now(),
					objectMapper.writeValueAsString(result)
			);
			return toDetailResponse(repository.save(entity));
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("No se pudo guardar el resultado del análisis.", exception);
		}
	}

	@Transactional(readOnly = true)
	public AnalysisHistoryPageResponse findPage(int page, int size) {
		int normalizedPage = Math.max(0, page);
		int normalizedSize = Math.min(Math.max(1, size), maxPageSize);
		Pageable pageable = PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<AnalysisSummaryResponse> results = repository
				.findAllByOwnerId(currentUserService.currentUserId(), pageable)
				.map(this::toSummaryResponse);

		return new AnalysisHistoryPageResponse(
				results.getContent(),
				results.getNumber(),
				results.getSize(),
				results.getTotalElements(),
				results.getTotalPages()
		);
	}

	@Transactional(readOnly = true)
	public AnalysisHistoryResponse findById(String id) {
		return repository.findByIdAndOwnerId(id, currentUserService.currentUserId())
				.map(this::toDetailResponse)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El análisis no existe."));
	}

	@Transactional
	public void deleteById(String id) {
		long deleted = repository.deleteByIdAndOwnerId(id, currentUserService.currentUserId());
		if (deleted == 0) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El análisis no existe.");
		}
	}

	private String limit(String value, int maxLength, String message) {
		String normalized = value == null ? "" : value.trim();
		if (normalized.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
		}
		if (normalized.length() > maxLength) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
		}
		return normalized;
	}

	private AnalysisSummaryResponse toSummaryResponse(AnalysisEntity entity) {
		return new AnalysisSummaryResponse(
				entity.getId(),
				entity.getRole(),
				entity.getCompany(),
				entity.getCvFileName(),
				entity.getCvVersion(),
				entity.getMode(),
				entity.getScore(),
				entity.getCreatedAt()
		);
	}

	private AnalysisHistoryResponse toDetailResponse(AnalysisEntity entity) {
		try {
			return new AnalysisHistoryResponse(
					entity.getId(), entity.getRole(), entity.getCompany(), entity.getCvFileName(),
					entity.getCvVersion(), entity.getJobDescription(), entity.getMode(), entity.getScore(),
					entity.getCreatedAt(), objectMapper.readValue(entity.getResultJson(), AnalysisResponse.class)
			);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("No se pudo leer un análisis guardado.", exception);
		}
	}
}
