package com.bmskinner.nuclear_morphology.analysis;

import static org.junit.Assert.assertEquals;

/**
 * Implements an equals method for float array comparisons.
 * @author bms41
 * @since 1.14.0
 *
 */
public abstract class FloatArrayTester {
	
	/**
	 * Test float array equality. Not in junit.
	 * @param exp the expected array
	 * @param obs the observed array
	 * @param epsilon the maximum difference between values
	 */
	public static boolean equals(float[] exp, float[] obs, float epsilon){
	    boolean equal = true;
	    equal &= obs.length==exp.length;
	    assertEquals(exp.length, obs.length);
        
        for(int i=0; i<exp.length; i++){
            equal &= (Float.isNaN(exp[i]) && Float.isNaN(obs[i])) || Math.abs(exp[i] - obs[i])<=epsilon;
            assertEquals("Index "+i, exp[i], obs[i], epsilon);
        }
        return equal;
	}

}
