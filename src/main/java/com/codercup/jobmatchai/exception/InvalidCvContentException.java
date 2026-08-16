package com.codercup.jobmatchai.exception;

public class InvalidCvContentException extends InvalidAnalysisRequestException {

	public InvalidCvContentException() {
		super("El archivo cargado no parece contener un currículum válido.");
	}
}
