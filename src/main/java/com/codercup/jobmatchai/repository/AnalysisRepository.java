package com.codercup.jobmatchai.repository;

import com.codercup.jobmatchai.entity.AnalysisEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRepository extends JpaRepository<AnalysisEntity, String> {

	Page<AnalysisEntity> findAllByOwnerId(String ownerId, Pageable pageable);

	Optional<AnalysisEntity> findByIdAndOwnerId(String id, String ownerId);

	long deleteByIdAndOwnerId(String id, String ownerId);
}
