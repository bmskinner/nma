package com.bmskinner.nma.components.measure;

import com.bmskinner.nma.components.Measurable;
import com.bmskinner.nma.components.MissingDataException;

/**
 * The exception class to be thrown when an expected measurement in a
 * {@link Measurable} object is not present
 * 
 * @author Ben Skinner
 * @since 2.2.0
 *
 */
public class MissingMeasurementException extends MissingDataException {

	private static final long serialVersionUID = 1L;

	public MissingMeasurementException() {
		super();
	}

	public MissingMeasurementException(String message) {
		super(message);
	}

	public MissingMeasurementException(String message, Throwable cause) {
		super(message, cause);
	}

	public MissingMeasurementException(Throwable cause) {
		super(cause);
	}
}