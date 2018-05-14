package com.bmskinner.nuclear_morphology.analysis.classification;

/**
 * This exception gets thrown when errors occur during a clustering
 * or classification
 * 
 * @author ben
 *
 */
public class ClusteringMethodException extends Exception {
    private static final long serialVersionUID = 1L;

    public ClusteringMethodException() {
        super();
    }

    public ClusteringMethodException(String message) {
        super(message);
    }

    public ClusteringMethodException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClusteringMethodException(Throwable cause) {
        super(cause);
    }
}
