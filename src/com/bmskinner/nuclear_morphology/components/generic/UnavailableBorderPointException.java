package com.bmskinner.nuclear_morphology.components.generic;

/**
 * Thrown when the requested border point is not present in a cellular component
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public class UnavailableBorderPointException extends UnavailableComponentException {
    private static final long serialVersionUID = 1L;

    public UnavailableBorderPointException() {
        super();
    }

    public UnavailableBorderPointException(String message) {
        super(message);
    }

    public UnavailableBorderPointException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnavailableBorderPointException(Throwable cause) {
        super(cause);
    }
}