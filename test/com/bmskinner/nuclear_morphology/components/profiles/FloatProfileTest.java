/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.components.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jdom2.Element;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class FloatProfileTest {
	
	private float[] data;
	private IProfile profile;
	

	@Before
	public void setUp(){
	    data       = new float[] { 10, 5, 1, 2, 7, 19, 12, 3, 9, 20, 13, 6, 4 }; // template data for a profile
	    profile = new FloatProfile(data);
	}
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.profiles.FloatProfile#FloatProfile(float[])}.
	 */
	@Test
	public void testFloatProfileFloatArrayWithNullData() {
		exception.expect(IllegalArgumentException.class);
		new FloatProfile( (float[]) null);
	}
	
	@Test
    public void testFloatProfileFloatIntExceptsWithLengthBelowZero() {
        exception.expect(IllegalArgumentException.class);
        new FloatProfile(5, -1);
    }
	
	@Test
    public void testFloatProfileFloatIntExceptsWithLengthZero() {
	    exception.expect(IllegalArgumentException.class);
        new FloatProfile(5, 0);
	}
		
	@Test
    public void testFloatProfileFloatIntSucceedsWithLengthTwo() {
        IProfile p = new FloatProfile(5, 2);       
        for( int i =0;i<p.size(); i++){
            assertEquals(5, p.get(i), 0);
        }   
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.profiles.FloatProfile#FloatProfile(com.bmskinner.nuclear_morphology.components.profiles.IProfile)}.
	 */
	@Test
	public void testFloatProfileIProfile() {

		IProfile tester = new FloatProfile(data);
		float[] result = new FloatProfile(tester).toFloatArray();
		
		for( int i =0;i<data.length; i++){
			assertEquals(data[i], result[i],0);
		}		
	}
	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.profiles.FloatProfile#FloatProfile(float, int)}.
	 */
	@Test
	public void testFloatProfileFloatInt() {
		
		int value = 1;
		int length = 6;
		
		float[] exp   = { 1, 1, 1, 1, 1, 1 };
		
		IProfile p  = new FloatProfile(value, length);
		
		float[] result = p.toFloatArray();
		
		for( int i =0;i<exp.length; i++){
			assertEquals(exp[i], result[i],0);
		}
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.profiles.FloatProfile#equals(java.lang.Object)}.
	 */
	@Test
	public void testEqualsObject() {
		IProfile p2 = new FloatProfile(data);
		assertTrue(profile.equals(p2));
		
	}
	
	/**
	 * Test float array equality. Not in junit.
	 * @param exp
	 * @param obs
	 */
	public static boolean equals(float[] exp, float[] obs){
	    
	    boolean equal = true;
	    equal &= obs.length==exp.length;
	    assertEquals(exp.length, obs.length);
        
        for(int i=0; i<exp.length; i++){
            equal &= exp[i] == obs[i];
            assertEquals(exp[i], obs[i], 0);
        }
        return equal;
	}
	
	@Test
	public void testXmlSerialiseas() {
		IProfile p2 = new FloatProfile(data);

		Element e = p2.toXmlElement();
		
		IProfile test = new FloatProfile(e);
		
		assertEquals(p2, test);
	}
	
}
