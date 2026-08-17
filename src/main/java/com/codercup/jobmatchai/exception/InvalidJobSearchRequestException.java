package com.codercup.jobmatchai.exception;

public class InvalidJobSearchRequestException extends RuntimeException {

	public InvalidJobSearchRequestException() {
		super("Los criterios de busqueda de ofertas no son validos.");
	}
}
