package com.bmskinner.nma.components.cells;

/**
 * Thrown when a component cannot be created or initialised (includes when
 * profile collection or segmented profile has no assigned segments)
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public class ComponentCreationException extends Exception {
    private static final long serialVersionUID = 1L;

    public ComponentCreationException() {
        super();
    }

    public ComponentCreationException(String message) {
        super(message);
    }

    public ComponentCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ComponentCreationException(Throwable cause) {
        super(cause);
    }

}
