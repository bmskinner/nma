package com.bmskinner.nuclear_morphology.analysis.profiles;

/**
 * Thrown when no indexes are found by a ruleset
 * 
 * @author bms41
 * @since 1.13.6
 *
 */
public class NoDetectedIndexException extends Exception {
    private static final long serialVersionUID = 1L;

    public NoDetectedIndexException() {
        super();
    }

    public NoDetectedIndexException(String message) {
        super(message);
    }

    public NoDetectedIndexException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoDetectedIndexException(Throwable cause) {
        super(cause);
    }
}