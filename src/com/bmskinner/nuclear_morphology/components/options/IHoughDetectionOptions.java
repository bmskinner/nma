package com.bmskinner.nuclear_morphology.components.options;

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;

/**
 * Describes the methods available for detecting
 * circles within images using the Hough algorithm.
 * @author ben
 * @since 1.13.4
 *
 */
public interface IHoughDetectionOptions extends IDetectionSubOptions {

	public static final String MIN_RADIUS = "Min radius";
	public static final String MAX_RADIUS = "Max radius";
	public static final String NUM_CIRCLES = "Number of circles";
	
	
	/**
	 * Get the minimum radius circle detected
	 * @return
	 */
	double getMinRadius();
	
	/**
	 * Get the maximum radius circle detected
	 * @return
	 */
	double getMaxRadius();
	
	/**
	 * Get the number of circles to be detected
	 * @return
	 */
	int getNumberOfCircles();
	
}
