package com.codercup.jobmatchai.exception;

public class InvalidCareerAiResponseException extends RuntimeException {

	public InvalidCareerAiResponseException() {
		super("No pudimos generar tus caminos profesionales en este momento.");
	}

	public InvalidCareerAiResponseException(Throwable cause) {
		super("No pudimos generar tus caminos profesionales en este momento.", cause);
	}
}
