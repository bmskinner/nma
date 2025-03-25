package com.bmskinner.nma.components.rules;

/**
 * Record which axis should be given priority for
 * orienting a nucleus when multiple orientation points
 * are present
 * @author Ben Skinner
 * @since 2.0.0
 */
public enum PriorityAxis {
	
	
	/**
	 * The object should be preferentially oriented according to X-axis landmarks
	 */
	X, 
	
	/**
	 * The object should be preferentially oriented according to Y-axis landmarks
	 */
	Y
}
