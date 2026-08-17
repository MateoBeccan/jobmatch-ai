package com.codercup.jobmatchai.exception;

public class JobSearchUnavailableException extends RuntimeException {

	public JobSearchUnavailableException(String message) {
		super(message);
	}

	public JobSearchUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
