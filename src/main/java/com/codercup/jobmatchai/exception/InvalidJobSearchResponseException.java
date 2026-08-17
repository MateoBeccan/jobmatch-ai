package com.codercup.jobmatchai.exception;

public class InvalidJobSearchResponseException extends RuntimeException {

	public InvalidJobSearchResponseException(String message) {
		super(message);
	}

	public InvalidJobSearchResponseException(String message, Throwable cause) {
		super(message, cause);
	}
}
