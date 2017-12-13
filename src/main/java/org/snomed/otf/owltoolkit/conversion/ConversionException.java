package org.snomed.otf.owltoolkit.conversion;

public class ConversionException extends Exception {
	public ConversionException(String message) {
		super(message);
	}

	public ConversionException(String message, Throwable cause) {
		super(message, cause);
	}
}
