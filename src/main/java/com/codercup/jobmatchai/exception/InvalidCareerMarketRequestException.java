package com.codercup.jobmatchai.exception;

public class InvalidCareerMarketRequestException extends RuntimeException {

	public InvalidCareerMarketRequestException() {
		super("Los criterios de orientacion profesional no son validos.");
	}
}
