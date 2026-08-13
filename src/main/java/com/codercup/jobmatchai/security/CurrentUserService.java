package com.codercup.jobmatchai.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

	private final boolean securityEnabled;

	public CurrentUserService(@Value("${security.enabled:true}") boolean securityEnabled) {
		this.securityEnabled = securityEnabled;
	}

	public String currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			if (!securityEnabled) {
				return "local-test-user";
			}
			throw new IllegalStateException("No hay un usuario autenticado.");
		}

		return authentication.getName();
	}
}
