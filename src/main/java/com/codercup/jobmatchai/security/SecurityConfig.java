package com.codercup.jobmatchai.security;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource,
			RateLimitFilter rateLimitFilter, @Value("${security.enabled:true}") boolean securityEnabled) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) -> writeError(response, 401, "Autenticacion requerida."))
						.accessDeniedHandler((request, response, exception) -> writeError(response, 403, "No tienes permisos para acceder a este recurso.")))
				.addFilterAfter(rateLimitFilter, BasicAuthenticationFilter.class);

		if (securityEnabled) {
			http.httpBasic(basic -> { });
		} else {
			http.httpBasic(AbstractHttpConfigurer::disable);
		}

		http.authorizeHttpRequests(authorize -> {
			authorize.requestMatchers("/actuator/health").permitAll();
			authorize.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
			authorize.requestMatchers(HttpMethod.POST, "/api/analyze").permitAll();
			authorize.requestMatchers(HttpMethod.POST, "/api/jobs/search").permitAll();
			if (securityEnabled) {
				authorize.requestMatchers("/api/analyses", "/api/analyses/**").hasRole("USER");
			} else {
				authorize.requestMatchers("/api/analyses", "/api/analyses/**").permitAll();
			}
			authorize.anyRequest().denyAll();
		});

		return http.build();
	}

	private void writeError(jakarta.servlet.http.HttpServletResponse response, int status, String message)
			throws java.io.IOException {
		response.setStatus(status);
		response.setContentType("application/json");
		response.getWriter().write("{\"message\":\"" + message + "\"}");
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(PasswordEncoder passwordEncoder,
			@Value("${security.demo-username:demo}") String username,
			@Value("${security.demo-password:demo-password}") String password) {
		return new InMemoryUserDetailsManager(User
				.withUsername(username)
				.password(passwordEncoder.encode(password))
				.roles("USER")
				.build());
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(@Value("${cors.allowed-origins}") String allowedOrigins) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isBlank())
				.toList());
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
		configuration.setAllowCredentials(false);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		source.registerCorsConfiguration("/actuator/health", configuration);
		return source;
	}
}
