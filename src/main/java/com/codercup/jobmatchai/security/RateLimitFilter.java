package com.codercup.jobmatchai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private final int requestsPerMinute;
	private final int careerRequestsPerMinute;
	private final Map<String, RequestWindow> windows = new ConcurrentHashMap<>();

	public RateLimitFilter(int requestsPerMinute) {
		this(requestsPerMinute, requestsPerMinute);
	}

	@org.springframework.beans.factory.annotation.Autowired
	public RateLimitFilter(
			@Value("${rate-limit.per-minute:10}") int requestsPerMinute,
			@Value("${rate-limit.career-per-minute:3}") int careerRequestsPerMinute
	) {
		if (requestsPerMinute < 1) {
			throw new IllegalArgumentException("El limite por minuto debe ser mayor a 0.");
		}
		if (careerRequestsPerMinute < 1) {
			throw new IllegalArgumentException("El limite career por minuto debe ser mayor a 0.");
		}
		this.requestsPerMinute = requestsPerMinute;
		this.careerRequestsPerMinute = careerRequestsPerMinute;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !"POST".equalsIgnoreCase(request.getMethod())
				|| !("/api/analyze".equals(request.getRequestURI())
						|| "/api/analyses".equals(request.getRequestURI())
						|| "/api/jobs/search".equals(request.getRequestURI())
						|| "/api/career/market".equals(request.getRequestURI())
						|| "/api/career/multiverse".equals(request.getRequestURI()));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String userId = rateLimitKey(request);
		long now = System.nanoTime();
		RequestWindow window = windows.compute(userId, (key, current) -> {
			if (current == null || now - current.startedAtNanos() >= TimeUnit.MINUTES.toNanos(1)) {
				return new RequestWindow(now, 1);
			}
			return current.increment();
		});

		if (window.count() > requestsPerMinute(request)) {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setHeader("Retry-After", "60");
			response.setContentType("application/json");
			response.getWriter().write(
					"{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"" + rateLimitMessage(request) + "\"}"
			);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private String currentUserId(HttpServletRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken)) {
			return authentication.getName();
		}

		String clientIp = request.getHeader("CF-Connecting-IP");
		if (clientIp != null && !clientIp.isBlank()) {
			return clientIp.trim();
		}

		return request.getRemoteAddr();
	}

	private String rateLimitKey(HttpServletRequest request) {
		String prefix = switch (request.getRequestURI()) {
			case "/api/jobs/search" -> "jobs:";
			case "/api/career/market", "/api/career/multiverse" -> "career:";
			default -> "analysis:";
		};
		return prefix + currentUserId(request);
	}

	private int requestsPerMinute(HttpServletRequest request) {
		if ("/api/career/market".equals(request.getRequestURI())
				|| "/api/career/multiverse".equals(request.getRequestURI())) {
			return careerRequestsPerMinute;
		}
		return requestsPerMinute;
	}

	private String rateLimitMessage(HttpServletRequest request) {
		if ("/api/jobs/search".equals(request.getRequestURI())) {
			return "Se supero el limite de busquedas de ofertas por minuto.";
		}
		if ("/api/career/market".equals(request.getRequestURI())
				|| "/api/career/multiverse".equals(request.getRequestURI())) {
			return "Se supero el limite de consultas de orientacion profesional por minuto.";
		}
		return "Se supero el limite de analisis por minuto.";
	}

	private static final class RequestWindow {
		private final long startedAtNanos;
		private final int count;

		private RequestWindow(long startedAtNanos, int count) {
			this.startedAtNanos = startedAtNanos;
			this.count = count;
		}

		private RequestWindow increment() {
			return new RequestWindow(startedAtNanos, count + 1);
		}

		private long startedAtNanos() { return startedAtNanos; }
		private int count() { return count; }
	}
}
