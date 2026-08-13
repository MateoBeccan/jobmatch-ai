package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.dto.AnalysisHistoryResponse;
import com.codercup.jobmatchai.dto.AnalysisResponse;
import com.codercup.jobmatchai.entity.AnalysisEntity;
import com.codercup.jobmatchai.repository.AnalysisRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AnalysisHistoryService {

	private final AnalysisRepository repository;
	private final ObjectMapper objectMapper;

	public AnalysisHistoryService(AnalysisRepository repository) {
		this.repository = repository;
		this.objectMapper = new ObjectMapper();
	}

	public AnalysisHistoryResponse save(String cvFileName, String cvVersion, String role, String company,
			String jobDescription, String mode, AnalysisResponse result) {
		try {
			AnalysisEntity entity = new AnalysisEntity(
					cvFileName,
					cvVersion,
					role,
					company,
					jobDescription,
					mode,
					result.matchPercentage(),
					Instant.now(),
					objectMapper.writeValueAsString(result)
			);
			return toResponse(repository.save(entity));
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("No se pudo guardar el resultado del análisis.", exception);
		}
	}

	public List<AnalysisHistoryResponse> findAll() {
		return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
	}

	public AnalysisHistoryResponse findById(String id) {
		return repository.findById(id)
				.map(this::toResponse)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El análisis no existe."));
	}

	public void deleteById(String id) {
		if (!repository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El análisis no existe.");
		}
		repository.deleteById(id);
	}

	private AnalysisHistoryResponse toResponse(AnalysisEntity entity) {
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