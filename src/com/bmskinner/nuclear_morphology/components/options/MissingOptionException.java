package com.bmskinner.nuclear_morphology.components.options;

/**
 * Thrown when an expected option is not found
 * @author ben
 *
 */
public class MissingOptionException extends Exception {
	private static final long serialVersionUID = 1L;
	public MissingOptionException() { super(); }
	public MissingOptionException(String message) { super(message); }
	public MissingOptionException(String message, Throwable cause) { super(message, cause); }
	public MissingOptionException(Throwable cause) { super(cause); }
}
