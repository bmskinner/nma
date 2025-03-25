package com.bmskinner.nma.components.rules;

/**
 * Markers that can be used to orient an object
 * 
 * @author Ben Skinner
 * @since 2.0.0
 *
 */
public enum OrientationMark {

	/**
	 * A landmark found left of the centre of mass of an oriented object
	 */
	LEFT, 
	
	/**
	 * A landmark found right of the centre of mass of an oriented object
	 */
	RIGHT, 
	
	/**
	 * A landmark found above the centre of mass of an oriented object
	 */
	TOP, 
	
	/**
	 * A landmark found below the centre of mass of an oriented object
	 */
	BOTTOM, 
	
	/**
	 * A landmark directly left of the centre of mass of an oriented object
	 */
	X, 
	/**
	 * A landmark directly below the centre of mass of an oriented object
	 */
	Y, 
	
	/**
	 * A landmark used as the starting index for profiles
	 */
	REFERENCE
}
