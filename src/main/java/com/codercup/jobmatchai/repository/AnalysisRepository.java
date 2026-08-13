package com.codercup.jobmatchai.repository;

import com.codercup.jobmatchai.entity.AnalysisEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRepository extends JpaRepository<AnalysisEntity, String> {
	List<AnalysisEntity> findAllByOrderByCreatedAtDesc();
}