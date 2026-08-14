package com.codercup.jobmatchai.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OwnershipSchemaInitializer implements ApplicationRunner {

	private final JdbcTemplate jdbcTemplate;
	private final String defaultOwner;

	public OwnershipSchemaInitializer(
			JdbcTemplate jdbcTemplate,
			@Value("${security.demo-username:demo}") String defaultOwner
	) {
		this.jdbcTemplate = jdbcTemplate;
		this.defaultOwner = defaultOwner;
	}

	@Override
	public void run(ApplicationArguments args) {
		jdbcTemplate.execute("ALTER TABLE analyses ADD COLUMN IF NOT EXISTS owner_id VARCHAR(120)");
		jdbcTemplate.update("UPDATE analyses SET owner_id = ? WHERE owner_id IS NULL OR owner_id = ''", defaultOwner);
		jdbcTemplate.execute("ALTER TABLE analyses ALTER COLUMN owner_id SET NOT NULL");
	}
}
