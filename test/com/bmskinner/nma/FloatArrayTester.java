package com.bmskinner.nma;

import static org.junit.Assert.assertEquals;

/**
 * Implements an equals method for float array comparisons.
 * @author Ben Skinner
 * @since 1.14.0
 *
 */
public abstract class FloatArrayTester {
	
	public static final float DEFAULT_EPSILON = 0;
	
	/**
	 * Test float array equality. Not in junit.
	 * @param exp the expected array
	 * @param obs the observed array
	 * @param epsilon the maximum difference between values
	 */
	public static boolean equals(float[] exp, float[] obs, float epsilon){
	    boolean equal = true;
	    equal &= obs.length==exp.length;
	    assertEquals("Array length", exp.length, obs.length);
        
        for(int i=0; i<exp.length; i++){
            equal &= (Float.isNaN(exp[i]) && Float.isNaN(obs[i])) || Math.abs(exp[i] - obs[i])<=epsilon;
            assertEquals("Index "+i, exp[i], obs[i], epsilon);
        }
        return equal;
	}
	
	/**
	 * Test float array equality with the default epsilon {@link #DEFAULT_EPSILON}
	 * @param exp the expected array
	 * @param obs the observed array
	 */
	public static boolean equals(float[] exp, float[] obs){
	    return equals(exp, obs, DEFAULT_EPSILON);
	}

}
