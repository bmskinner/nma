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
	
	/**
	 * Adds the mutable settings to the options
	 * @author ben
	 *
	 */
	public interface IMutableHoughDetectionOptions extends IHoughDetectionOptions {
		
		IHoughDetectionOptions lock();
		
		void setMinRadius(double d);
		
		void setMaxRadius(double d);
		
		/**
		 * Set the maximum number of circles to return. This will have no effect
		 * if the hough threshold is greater than -1.
		 * @param i
		 */
		void setNumberOfCircles(int i);
		
		/**
		 * Set the Hough space threshold for circle detection. Alternative
		 * to setting the max number of circles.
		 * @param i the threshold from 0-255. Set to -1 to disable and use max circles
		 */
		void setHoughThreshold(int i);
	}

	public static final String MIN_RADIUS = "Min radius";
	public static final String MAX_RADIUS = "Max radius";
	public static final String NUM_CIRCLES = "Number of circles";
	public static final String HOUGH_THRESHOLD = "Hough threshold";
	
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
	
	/**
	 * Get the Hough space threshold for circle detection, if set,
	 * or -1 if not set.
	 * @return
	 */
	int getHoughThreshold();
	
}
