package com.bmskinner.nuclear_morphology.utility;

/**
 * Utility class to help manage common number tasks
 * @author ben
 * @since 1.19.4
 *
 */
public class NumberTools {
	
	/**
	 * Constrain a number to within set bounds
	 * @param number the number to constrain
	 * @param min the minimum value
	 * @param max the maximum value
	 * @return the minimum or maximum bound if the number is outside these
	 */
	public static int constrain(int number, int min, int max) {
		number = number>max ? max : number;
		number = number<min ? min :number;
		return number;
	}

}
