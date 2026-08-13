package com.codercup.jobmatchai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mock.env.MockEnvironment;

class DeploymentConfigurationTest {

	@Test
	void serverPortUsesLocalDefaultWhenPortIsNotProvided() throws IOException {
		MockEnvironment environment = environmentFromApplicationProperties();

		assertThat(environment.getProperty("server.port")).isEqualTo("8080");
	}

	@Test
	void serverPortCanBeConfiguredFromPortProperty() throws IOException {
		MockEnvironment environment = environmentFromApplicationProperties()
				.withProperty("PORT", "9090");

		assertThat(environment.getProperty("server.port")).isEqualTo("9090");
	}

	@Test
	void serverAddressBindsToAllInterfacesForContainerDeployments() throws IOException {
		MockEnvironment environment = environmentFromApplicationProperties();

		assertThat(environment.getProperty("server.address")).isEqualTo("0.0.0.0");
	}

	private MockEnvironment environmentFromApplicationProperties() throws IOException {
		Properties properties = PropertiesLoaderUtils.loadProperties(
				new ClassPathResource("application.properties")
		);
		MockEnvironment environment = new MockEnvironment();
		properties.forEach((name, value) -> environment.withProperty(name.toString(), value.toString()));
		return environment;
	}
}
