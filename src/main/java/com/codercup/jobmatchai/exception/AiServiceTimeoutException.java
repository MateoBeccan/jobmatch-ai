package com.codercup.jobmatchai.exception;

public class AiServiceTimeoutException extends RuntimeException {

	public AiServiceTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}
}
