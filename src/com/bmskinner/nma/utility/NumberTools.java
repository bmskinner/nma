package com.bmskinner.nma.utility;

/**
 * Utility class to help manage common number tasks
 * 
 * @author ben
 * @since 1.19.4
 *
 */
public class NumberTools {

	private NumberTools() {
		// only uses static methods
	}

	/**
	 * Constrain a number to within set bounds.
	 * 
	 * @param number the number to constrain
	 * @param min    the minimum value
	 * @param max    the maximum value
	 * @return the minimum or maximum bound if the number is outside these
	 */
	public static int constrain(int number, int min, int max) {
		if (number > max)
			return max;
		if (number < min)
			return min;
		return number;
	}

}
