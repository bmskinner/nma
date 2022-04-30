package com.bmskinner.nma.analysis;

/**
 * Generic exception for errors in analysis methods
 * @author ben
 * @since 2.0.0
 *
 */
public class AnalysisMethodException extends Exception {
	private static final long serialVersionUID = 1L;

    public AnalysisMethodException() {
        super();
    }

    public AnalysisMethodException(String message) {
        super(message);
    }

    public AnalysisMethodException(String message, Throwable cause) {
        super(message, cause);
    }

    public AnalysisMethodException(Throwable cause) {
        super(cause);
    }
}
