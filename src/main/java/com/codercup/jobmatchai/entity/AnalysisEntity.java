package com.codercup.jobmatchai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "analyses")
public class AnalysisEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(name = "owner_id", length = 120)
	private String ownerId;

	@Column(nullable = false, length = 160)
	private String cvFileName;

	@Column(nullable = false, length = 120)
	private String cvVersion;

	@Column(nullable = false, length = 160)
	private String role;

	@Column(nullable = false, length = 120)
	private String company;

	@Lob
	@Column(nullable = false)
	private String jobDescription;

	@Column(nullable = false, length = 16)
	private String mode;

	@Column(nullable = false)
	private Integer score;

	@Column(nullable = false)
	private Instant createdAt;

	@Lob
	@Column(nullable = false)
	private String resultJson;

	protected AnalysisEntity() {
	}

	public AnalysisEntity(String ownerId, String cvFileName, String cvVersion, String role, String company,
			String jobDescription, String mode, Integer score, Instant createdAt, String resultJson) {
		this.ownerId = ownerId;
		this.cvFileName = cvFileName;
		this.cvVersion = cvVersion;
		this.role = role;
		this.company = company;
		this.jobDescription = jobDescription;
		this.mode = mode;
		this.score = score;
		this.createdAt = createdAt;
		this.resultJson = resultJson;
	}

	public String getId() { return id; }
	public String getOwnerId() { return ownerId; }
	public String getCvFileName() { return cvFileName; }
	public String getCvVersion() { return cvVersion; }
	public String getRole() { return role; }
	public String getCompany() { return company; }
	public String getJobDescription() { return jobDescription; }
	public String getMode() { return mode; }
	public Integer getScore() { return score; }
	public Instant getCreatedAt() { return createdAt; }
	public String getResultJson() { return resultJson; }
}
