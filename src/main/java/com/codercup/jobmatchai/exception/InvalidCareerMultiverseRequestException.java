package com.codercup.jobmatchai.exception;

public class InvalidCareerMultiverseRequestException extends RuntimeException {

	public InvalidCareerMultiverseRequestException() {
		super("Los criterios de Career Multiverse no son validos.");
	}
}
