package org.snomed.otf.owltoolkit.service;

public class ReasonerServiceRuntimeException extends RuntimeException {

	public ReasonerServiceRuntimeException(String message) {
		super(message);
	}

	public ReasonerServiceRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public ReasonerServiceRuntimeException(Throwable cause) {
		super(cause);
	}
}
