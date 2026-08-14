package com.codercup.jobmatchai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

class DeploymentSupabaseConfigurationTest {

	@Test
	void supabaseProfileUsesSecurePostgresAndFlywayValidation() throws IOException {
		Properties properties = PropertiesLoaderUtils.loadProperties(
				new ClassPathResource("application-supabase.properties")
		);

		assertThat(properties.getProperty("spring.config.activate.on-profile")).isEqualTo("supabase");
		assertThat(properties.getProperty("spring.datasource.hikari.data-source-properties.sslmode"))
				.isEqualTo("require");
		assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
		assertThat(properties.getProperty("spring.flyway.enabled")).isEqualTo("true");
		assertThat(properties.getProperty("spring.jpa.properties.hibernate.jdbc.time_zone")).isEqualTo("UTC");
	}
}
