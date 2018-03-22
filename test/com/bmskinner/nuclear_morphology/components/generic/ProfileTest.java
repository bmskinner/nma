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
package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;

import static org.hamcrest.CoreMatchers.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.generic.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.generic.FloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;


public class ProfileTest {
	
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
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#FloatProfile(float[])}.
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
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#FloatProfile(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 */
	@Test
	public void testFloatProfileIProfile() {

		IProfile tester = new FloatProfile(data);
		float[] result = new FloatProfile(tester).toFloatArray();
		
		for( int i =0;i<data.length; i++){
			assertEquals(data[i], result[i],0);
		}		
	}
	
	@Test
    public void testFloatProfileIProfileExceptsOnNullProfile() {
        exception.expect(IllegalArgumentException.class);
        new FloatProfile( (IProfile)null);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#FloatProfile(float, int)}.
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
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#size()}.
	 */
	@Test
	public void testSize() {
		assertEquals(profile.size(), data.length);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#equals(java.lang.Object)}.
	 */
	@Test
	public void testEqualsObject() {
		IProfile p2 = new FloatProfile(data);
		assertTrue(profile.equals(p2));
		
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#get(int)}.
	 */
	@Test
	public void testGetInt() {
		IProfile p1 = new FloatProfile(data);
		double d = p1.get(4);
		assertEquals( 7d , d, 0);
	}
	
	@Test
	public void testGetIntExceptsOnNegativeIndex() {
	    exception.expect(IndexOutOfBoundsException.class);
        profile.get(-1);
	}
	
	@Test
    public void testGetIntExceptsOnOutOfBoundsIndex() {
        exception.expect(IndexOutOfBoundsException.class);
        profile.get(100);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#get(double)}.
	 */
	@Test
	public void testGetDouble() {
		double d = profile.get(0.5);
		assertEquals(12, d, 0);
	}
	
	@Test
    public void testGetDoubleExceptsWhenProportionOutOfLowerBounds() {
	    exception.expect(IndexOutOfBoundsException.class);
        profile.get(-0.1);
    }
	
	@Test
	public void testGetDoubleExceptsWhenProportionOutOfUpperBounds() {
	    exception.expect(IndexOutOfBoundsException.class);
	    profile.get(1.1);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getMax()}.
	 */
	@Test
	public void testGetMax() {
		assertEquals( 20d , profile.getMax(), 0);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getIndexOfMax(com.bmskinner.nuclear_morphology.components.generic.BooleanProfile)}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetIndexOfMaxBooleanProfile() throws ProfileException {

	    // Restrict to first half of array
	    BooleanProfile b = new BooleanProfile(data.length, false);
	    for(int i=0; i<data.length/2; i++){
	        b.set(i, true);
	    }

	    int expected = 5;


	    IProfile p1 = new FloatProfile(data);
	    int index = p1.getIndexOfMax(b);
	    assertEquals( expected , index);
	}
	
	@Test
    public void testGetIndexOfMaxBooleanProfileExceptsOnNullProfile() throws ProfileException{
	    exception.expect(IllegalArgumentException.class);
	    profile.getIndexOfMax(null);
	}
	
	@Test
    public void testGetIndexOfMaxBooleanProfileExceptsOnAllFalseProfile() throws ProfileException{
	    BooleanProfile b = new BooleanProfile(data.length, false);
        exception.expect(ProfileException.class);
        profile.getIndexOfMax(b);
    }
	
	@Test
    public void testGetIndexOfMaxBooleanProfileExceptsOnDifferentLength() throws ProfileException{
        BooleanProfile b = new BooleanProfile(data.length/2, false);
        exception.expect(IllegalArgumentException.class);
        profile.getIndexOfMax(b);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getIndexOfMax()}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetIndexOfMax() throws ProfileException {
		IProfile p1 = new FloatProfile(data);
		assertEquals( 9 , p1.getIndexOfMax());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getIndexOfFraction(double)}.
	 */
	@Test
	public void testGetIndexOfFraction() {
		
		IProfile p1 = new FloatProfile(data);
		
		double fraction = 0.5;
		int exp = 6;
		int i = p1.getIndexOfFraction(fraction);
		assertEquals( 6, i);
		
	}
	
	@Test
    public void testGetIndexOfFractionExceptsOnLessThanZero() {
	    exception.expect(IllegalArgumentException.class);
	    profile.getIndexOfFraction(-0.1);
	}
	
	@Test
    public void testGetIndexOfFractionExceptsOnGreaterThanOne() {
        exception.expect(IllegalArgumentException.class);
        profile.getIndexOfFraction(1.1);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getFractionOfIndex(int)}.
	 */
	@Test
	public void testGetFractionOfIndex() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getMin()}.
	 */
	@Test
	public void testGetMin() {
		IProfile p1 = new FloatProfile(data);
		assertEquals( 1d , p1.getMin(), 0);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getIndexOfMin(com.bmskinner.nuclear_morphology.components.generic.BooleanProfile)}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetIndexOfMinBooleanProfile() throws ProfileException {
	    // Restrict to second half of array
	    BooleanProfile b = new BooleanProfile(data.length, true);
	    for(int i=0; i<data.length/2; i++){
	        b.set(i, false);
	    }

	    int expected = 7; // index

	    IProfile p1 = new FloatProfile(data);
	    int index = p1.getIndexOfMin(b);
	    assertEquals( expected , index);

	}
	
	@Test
    public void testGetIndexOfMinBooleanProfileExceptsOnNullProfile() throws ProfileException{
        exception.expect(IllegalArgumentException.class);
        profile.getIndexOfMin(null);
    }
    
    @Test
    public void testGetIndexOfMinBooleanProfileExceptsOnAllFalseProfile() throws ProfileException{
        BooleanProfile b = new BooleanProfile(data.length, false);
        exception.expect(ProfileException.class);
        profile.getIndexOfMin(b);
    }
	
	@Test
    public void testGetIndexOfMinBooleanProfileExceptsOnDifferentLength() throws ProfileException{
        BooleanProfile b = new BooleanProfile(data.length/2, false);
        exception.expect(IllegalArgumentException.class);
        profile.getIndexOfMin(b);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getIndexOfMin()}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetIndexOfMin() throws ProfileException {
		IProfile p1 = new FloatProfile(data);
		assertEquals( 2 , p1.getIndexOfMin());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#toFloatArray()}.
	 */
	@Test
	public void testToFloatArray() {
		IProfile p1 = new FloatProfile(data);
		
		float[] result = p1.toFloatArray();
		
		for( int i =0;i<data.length; i++){
			assertEquals(data[i], result[i],0);
		}
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#toDoubleArray()}.
	 */
	@Test
	public void testToDoubleArray() {
	    
	    double[] d = new double[data.length];
	    for(int i=0; i<data.length; i++){
	        d[i] = data[i];
	    }
	    
	    double[] res = profile.toDoubleArray();
	    
	    assertTrue(Arrays.equals(d, res));
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#absoluteSquareDifference(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 */
	@Test
	public void testAbsoluteSquareDifferenceIsZeroWhenSameProfile() throws ProfileException {
		assertEquals(0, profile.absoluteSquareDifference(profile), 0);
	}
	
	private void testAbsoluteSquareDifferenceOnSameLengthProfiles(IProfile template, float diff) throws ProfileException{
	    float[] test =  Arrays.copyOf(template.toFloatArray(), template.size());
        
        test[0] = test[0]+diff;
        
        IProfile p = new FloatProfile(test);
        
        double expDiff = diff*diff;
        
        assertEquals(expDiff, template.absoluteSquareDifference(p), 0);
	}
	
	@Test
    public void testAbsoluteSquareDifferenceOnSameLengthProfilesPositive() throws ProfileException {
	    testAbsoluteSquareDifferenceOnSameLengthProfiles(profile, 2);
    }
	
	@Test
    public void testAbsoluteSquareDifferenceOnSameLengthProfilesNegative() throws ProfileException {
	    testAbsoluteSquareDifferenceOnSameLengthProfiles(profile, -2);
    }
	
	private void testAbsoluteSquareDifferenceOnDifferentLengthProfiles(IProfile template, int newLength, float diff) throws ProfileException{

	    IProfile t = template.interpolate(newLength);
	    
	    float[] arr = t.toFloatArray();
	    arr[0] = arr[0]+diff;
        
	    IProfile p = new FloatProfile(arr);
	    System.out.println(template.toString());
	    System.out.println(p.toString());
        
        double expDiff = diff*diff;
        
        assertEquals(expDiff, template.absoluteSquareDifference(p), 0);
	}
	
	@Test
    public void testAbsoluteSquareDifferenceOnLongerProfilesPositive() throws ProfileException {
	    testAbsoluteSquareDifferenceOnDifferentLengthProfiles(profile, profile.size()*2, 2);
    }
	
	@Test
    public void testAbsoluteSquareDifferenceOnShorterProfilesPositive() throws ProfileException {
        testAbsoluteSquareDifferenceOnDifferentLengthProfiles(profile, profile.size()/2, 2);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#copy()}.
	 */
	@Test
	public void testCopy() {
		IProfile p = new FloatProfile(data);
		float[] result = p.copy().toFloatArray();

		assertTrue(Arrays.equals(data, result));
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#offset(int)}.
	 */
	@Test
	public void testOffset() throws ProfileException {

	    float[] exp1       = { 5, 1, 2, 7, 19, 12, 3, 9, 20, 13, 6, 4, 10 };
	    float[] exp5       = { 19, 12, 3, 9, 20, 13, 6, 4, 10, 5, 1, 2, 7 };
	    float[] exp_1      = { 4, 10, 5, 1, 2, 7, 19, 12, 3, 9, 20, 13, 6 }; // negative 1


	    IProfile p = new FloatProfile(data);
	    float[] result = p.offset(1).toFloatArray();

	    for( int i =0;i<data.length; i++){
	        assertEquals(exp1[i], result[i],0);
	    }


	    result = p.offset(5).toFloatArray();
	    for( int i =0;i<data.length; i++){
	        assertEquals(exp5[i], result[i],0);
	    }

	    result = p.offset(-1).toFloatArray();
	    for( int i =0;i<data.length; i++){
	        assertEquals(exp_1[i], result[i],0);
	    }

	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#smooth(int)}.
	 */
	@Test
	public void testSmooth() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#reverse()}.
	 */
	@Test
	public void testReverse() {
	    
	    float[] arr = Arrays.copyOf(data, data.length);
	    
	    for(int i = 0; i < arr.length / 2; i++) {
	        float temp = arr[i];
	        arr[i] = arr[arr.length - i - 1];
	        arr[arr.length - i - 1] = temp;
	    }
	    	    
	    profile.reverse();
	    
	    float[] res = profile.toFloatArray();
	    
	    for( int i =0;i<arr.length; i++){
            assertEquals(arr[i], res[i],0);
        }

		
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getSlidingWindowOffset(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetSlidingWindowOffset() throws ProfileException {
		
		/*
		 * Testing with equal array lengths
		 */
		
		float[] test       = { 9, 20, 13, 6, 4, 10, 5, 1, 2, 7, 19, 12, 3 };

		int expectedOffset = 8;
		
		IProfile dataProfile = new FloatProfile(data);
		IProfile templateProfile = new FloatProfile(test);

		
		int offset = dataProfile.getSlidingWindowOffset(templateProfile);


		assertEquals(expectedOffset, offset,0);
		
		/*
		 * Testing with shorter test array
		 */
		
		float[] smallTest  = { 9, 16, 5, 7, 1.5f, 13, 7 };
		IProfile pTest = new FloatProfile(smallTest);
		
		offset =  dataProfile.getSlidingWindowOffset(pTest);
		assertEquals(expectedOffset, offset,0);
		
		/*
		 * Testing with longer test array
		 */
		float[] longTest  = { 9, 14, 20, 16, 13, 9, 6, 5, 4, 7, 10, 7, 5, 2, 1, 1.5f, 2, 4, 7, 13, 19, 15, 12, 7, 3 };
		pTest = new FloatProfile(longTest);
		offset = dataProfile.getSlidingWindowOffset(pTest);
		assertEquals(expectedOffset, offset,0);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getConsistentRegionBounds(double, double, int)}.
	 */
	@Test
	public void testGetConsistentRegionBounds() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getLocalMinima(int)}.
	 */
	@Test
	public void testGetLocalMinimaInt() {
	    
	    float[] arr  = { 10, 9, 8, 7, 6, 5, 6, 7, 8, 9, 10 };
	    
	    IProfile p = new FloatProfile(arr);
	    
	    BooleanProfile b = p.getLocalMinima(3);
	    
	    assertTrue(b.get(5));
	    assertFalse(b.get(4));
	    assertFalse(b.get(6));
	}
	
	@Test
    public void testGetLocalMinimaIntExceptsOnZeroWindowSize() {
        exception.expect(IllegalArgumentException.class);
        profile.getLocalMinima(0);
    }
	
	private BooleanProfile testGetLocalMinima(int window, double threshold){
	    float[] arr  = { 10, 9, 8, 7, 6, 5, 6, 7, 8, 9, 10, 9, 8, 7, 8, 9, 10 };
        IProfile p = new FloatProfile(arr);
        return p.getLocalMinima(window, threshold);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getLocalMinima(int, double)}.
	 */
	@Test
	public void testGetLocalMinimaIntDoubleWithBothPassingThreshold() {
	    BooleanProfile b = testGetLocalMinima(3, 8);
	    assertTrue(b.get(5));
	    assertTrue(b.get(13));
	    assertFalse(b.get(4));
	    assertFalse(b.get(6));
	    assertFalse(b.get(12));
	    assertFalse(b.get(14));
	}
	
	@Test
    public void testGetLocalMinimaIntDoubleWithOnePassingThreshold() {
	    BooleanProfile b = testGetLocalMinima(3, 6);

        assertTrue(b.get(5));
        assertFalse(b.get(13));
        assertFalse(b.get(4));
        assertFalse(b.get(6));
        assertFalse(b.get(12));
        assertFalse(b.get(14));
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getLocalMinima(int, double, double)}.
	 */
	@Test
	public void testGetLocalMinimaIntDoubleDouble() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getLocalMaxima(int)}.
	 */
	@Test
    public void testGetLocalMaximaInt() {
        
        float[] arr  = { 5, 6, 7, 8, 9, 10, 9, 8, 7, 6, 5 };
        
        IProfile p = new FloatProfile(arr);
        
        BooleanProfile b = p.getLocalMaxima(3);
        
        assertTrue(b.get(5));
        assertFalse(b.get(4));
        assertFalse(b.get(6));
    }
    
    @Test
    public void testGetLocalMaximaIntExceptsOnZeroWindowSize() {
        exception.expect(IllegalArgumentException.class);
        profile.getLocalMaxima(0);
    }
    
    private BooleanProfile testGetLocalMaxima(int window, double threshold){
        float[] arr  = { 5, 6, 7, 8, 9, 10, 9, 8, 7, 6, 5, 6, 7, 8, 7, 6 };
        IProfile p = new FloatProfile(arr);
        return p.getLocalMaxima(window, threshold);
    }
    
    @Test
    public void testGetLocalMaximaIntDoubleWithBothPassingThreshold() {
        BooleanProfile b = testGetLocalMaxima(3, 4);
        assertTrue(b.get(5));
        assertTrue(b.get(13));
        assertFalse(b.get(4));
        assertFalse(b.get(6));
        assertFalse(b.get(12));
        assertFalse(b.get(14));
    }
    
    @Test
    public void testGetLocalMaximaIntDoubleWithOnePassingThreshold() {
        BooleanProfile b = testGetLocalMaxima(3, 9);

        assertTrue(b.get(5));
        assertFalse(b.get(13));
        assertFalse(b.get(4));
        assertFalse(b.get(6));
        assertFalse(b.get(12));
        assertFalse(b.get(14));
    }


	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getLocalMaxima(int, double, double)}.
	 */
	@Test
	public void testGetLocalMaximaIntDoubleDouble() {
		fail("Not yet implemented");
	}


	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getSubregion(int, int)}.
	 */
	@Test
	public void testGetSubregionIntInt() {		
		int start = 0;
		int stop  = 3;
		IProfile p = profile.getSubregion(start, stop);
		
		for(int i=start; i<stop; i++){
		    assertEquals(data[i], p.get(i), 0);
		}
	}
	
	@Test
    public void testGetSubregionIntIntWraps() {      
        int start = 8;
        int stop  = 2;
        IProfile p = profile.getSubregion(start, stop);
        
        System.out.println(p);
        
        for(int i=start; i<profile.size(); i++){
            System.out.println(i+" : "+data[i]);
            assertEquals(data[i], p.get(i-start), 0);
        }
        for(int i=0; i<stop; i++){
            System.out.println(i+" : "+data[i]);
            assertEquals(data[i], p.get(p.size()-stop+i), 0);
        }

    }
	
	@Test
    public void testGetSubregionIntIntExceptsOnLowerIndexOutOfBounds() {  
	    exception.expect(IllegalArgumentException.class);
	    profile.getSubregion(-1, 3);
	}
	
	@Test
    public void testGetSubregionIntIntExceptsOnUpperIndexOutOfBounds() {  
        exception.expect(IllegalArgumentException.class);
        profile.getSubregion(-1, profile.size()+1);
    }
	
	@Test
    public void testGetSubregionIntIntExceptsOnWrappingLowerIndexOutOfBounds() {  
        exception.expect(IllegalArgumentException.class);
        profile.getSubregion(profile.size()+1, 3);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getSubregion(com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment)}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetSubregionIBorderSegment() throws ProfileException {
	    int start = 0;
        int stop  = 3;
        IBorderSegment s = new DefaultBorderSegment(start, stop, data.length);
        IProfile p = profile.getSubregion(s);
        
        for(int i=start; i<stop; i++){
            assertEquals(data[i], p.get(i), 0);
        }
	}
	
	@Test
    public void testGetSubregionIBorderSegmentExceptsOnNullSegment() throws ProfileException{
	    exception.expect(IllegalArgumentException.class);
	    profile.getSubregion(null);
	}
		
	@Test
    public void testGetSubregionIBorderSegmentExceptsOnSegmentOutOfUpperBounds() throws ProfileException{
        IBorderSegment s = new DefaultBorderSegment(0, 100, 200);
        exception.expect(IllegalArgumentException.class);
        profile.getSubregion(s);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#calculateDeltas(int)}.
	 */
	@Test
	public void testCalculateDeltas() {
	    
	    IProfile res = profile.calculateDeltas(2);
//	    { 10, 5, 1, 2, 7, 19, 12, 3, 9, 20, 13, 6, 4 }; // template data for a profile
		fail("Not yet implemented");
	}


	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#power(double)}.
	 */
	@Test
	public void testPower() {
		float[] data    = { 1, 2, 3, 4, 5};
		double d = 2;
		float[] exp     = { 1, 4,  9, 16, 25};
		
		IProfile p1 = new FloatProfile(data);
		
		float[] result = p1.power(d).toFloatArray();
		
		for( int i =0;i<data.length; i++){
			assertEquals(exp[i], result[i],0);
		}
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#absolute()}.
	 */
	@Test
	public void testAbsolute() {
		float[] data    = { 1, 0, -1, 20, -20};
		float[] exp     = { 1, 0,  1, 20,  20};
		
		IProfile p1 = new FloatProfile(data);
		
		float[] result = p1.absolute().toFloatArray();
		
		for( int i =0;i<data.length; i++){
			assertEquals(exp[i], result[i],0);
		}
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#cumulativeSum()}.
	 */
	@Test
	public void testCumulativeSum() {
		float[] data    = { 1, 2, 3, 4, 5};
		float[] exp     = { 1, 3,  6, 10, 15};
		
		IProfile p1 = new FloatProfile(data);
		
		float[] result = p1.cumulativeSum().toFloatArray();
		
		for( int i =0;i<data.length; i++){
			assertEquals(exp[i], result[i],0);
		}
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#multiply(double)}.
	 */
	@Test
	public void testMultiplyDouble() {
		// MyClass is tested
		float[] data     = {1, 1, 1, 1, 1, 1};
		double   constant = 2;
		float[] expected = {2 ,2, 2, 2, 2, 2};

		IProfile tester = new FloatProfile(data);
		IProfile result = tester.multiply(constant);

		// assert statements

		// there is no assertArrayEquals for float[]
		for( int i =0;i<data.length; i++){
			assertEquals("1x2 should be 2", expected[i], result.toFloatArray()[i],0);
		}
	}
	
	@Test
	public void testMultiplyDoubleNanInputFails() {
	    exception.expect(IllegalArgumentException.class);
	    profile.multiply(Double.NaN);
	}


	@Test
	public void testMultiplyDoublePositiveInfinityInputFails() {
	    exception.expect(IllegalArgumentException.class);
	    profile.multiply(Double.POSITIVE_INFINITY);
	}

	@Test
	public void testMultiplyDoubleNegativeInfinityInputFails() {
	    exception.expect(IllegalArgumentException.class);
	    profile.multiply(Double.NEGATIVE_INFINITY);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#multiply(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 */
	@Test
	public void testMultiplyIProfile() {

		float[] data       = {0, 1, 2, 3,  4,  5 };
		float[] multiplier = {1, 2, 3, 4,  5,  6 };
		float[] expected   = {0, 2, 6, 12, 20, 30};

		IProfile tester = new FloatProfile(data);
		IProfile multiply = new FloatProfile(multiplier);
		IProfile result = tester.multiply(multiply);

		// there is no assertArrayEquals for float[]
		for( int i =0;i<data.length; i++){
			assertEquals(data[i]+"x"+multiplier[i]+"should be "+expected[i], expected[i], result.toFloatArray()[i],0);
		}
	}
	
	@Test
    public void testMultiplyProfileExceptsOnDifferentLength() {
        float[] f    = { 10, 10 };
        IProfile p2 = new FloatProfile(f);
        exception.expect(IllegalArgumentException.class);
        IProfile p3 = profile.multiply(p2);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#divide(double)}.
	 */
	@Test
	public void testDivideDouble() {
		float[] data       = {0, 1, 2, 3,  4,  5 };
		double   constant   = 2;
		float[] expected   = {0f, 0.5f, 1f, 1.5f, 2f, 2.5f};

		IProfile tester = new FloatProfile(data);
		IProfile result = tester.divide(constant);

		// there is no assertArrayEquals for float[]
		for( int i =0;i<data.length; i++){
			assertEquals(data[i]+"/"+constant+" should be "+expected[i], expected[i], result.toFloatArray()[i],0);
		}		
	}
	
	@Test
    public void testDivideDoubleNegative() {
        float[] data       = {0, 1, 2, 3,  4,  5 };
        double   constant   = 2;
        float[] expected   = {0f, 0.5f, 1f, 1.5f, 2f, 2.5f};

        IProfile tester = new FloatProfile(data);
        IProfile result = tester.divide(constant);
        
        tester = new FloatProfile(data);
        constant   = -2;
        float[] negatives   = {0f, -0.5f, -1f, -1.5f, -2f, -2.5f};
        result = tester.divide(constant);

        for( int i =0;i<data.length; i++){
            assertEquals(data[i]+"/"+constant+" should be "+negatives[i], negatives[i], result.toFloatArray()[i],0);
        }
        
        
    }
	
	@Test
    public void testDivideDoubleNanInputFails() {
        exception.expect(IllegalArgumentException.class);
        profile.divide(Double.NaN);
    }

	
	@Test
    public void testDivideDoublePositiveInfinityInputFails() {
        exception.expect(IllegalArgumentException.class);
        profile.divide(Double.POSITIVE_INFINITY);
    }
	
	@Test
    public void testDivideDoubleNegativeInfinityInputFails() {
        exception.expect(IllegalArgumentException.class);
        profile.divide(Double.NEGATIVE_INFINITY);
    }
	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#divide(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 */
	@Test
	public void testDivideIProfile() {
	    float[] data  = {0, 1, 2,   3,  4,  5 };
	    float[] div   = {1, 2, 0.5f, 3,  0.25f,     2 };
        float[] exp   = {0f, 0.5f, 4f, 1f, 16f, 2.5f};

        IProfile tester  = new FloatProfile(data);
        IProfile divider = new FloatProfile(div);
        IProfile result = tester.divide(divider);

        for( int i =0;i<data.length; i++){
            assertEquals(data[i]+"/"+div[i], exp[i], result.toFloatArray()[i],0);
        }   
	}
	
	@Test
    public void testDivideProfileExceptsOnDifferentLength() {
        float[] f    = { 10, 10 };
        IProfile p2 = new FloatProfile(f);
        exception.expect(IllegalArgumentException.class);
        IProfile p3 = profile.divide(p2);
    }
	
	@Test
    public void testDivideIProfileFailsOnSizeMismatch() {

        float[] div   = {1, 2, 0.5f, 3,  0.25f };
        IProfile divider = new FloatProfile(div);
        exception.expect(IllegalArgumentException.class);
        profile.divide(divider);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#add(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 */
	@Test
	public void testAddIProfile() {
		
		float[] exp       = { 20, 10, 2, 4, 14, 38, 24, 6, 18, 40, 26, 12, 8 }; // template data for a profile
		IProfile p1 = new FloatProfile(data);
		IProfile p2 = new FloatProfile(data);
		
		IProfile p3 = p1.add(p2);
		float[] result = p3.toFloatArray();
		
		for( int i =0;i<exp.length; i++){
			assertEquals(exp[i], result[i], 0);
		}
	}

	@Test
	public void testAddProfileExceptsOnDifferentLength() {
	    float[] f    = { 10, 10 };
	    IProfile p2 = new FloatProfile(f);
	    exception.expect(IllegalArgumentException.class);
	    IProfile p3 = profile.add(p2);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#add(double)}.
	 */
	@Test
	public void testAddDouble() {

		float[] data       = {0, 1, 2, 3,  4,  5 };
		double   constant   = 2;
		float[] expected   = {2, 3, 4, 5, 6, 7};

		IProfile tester = new FloatProfile(data);
		IProfile result = tester.add(constant);

		// there is no assertArrayEquals for float[]
		for( int i =0;i<data.length; i++){
			assertEquals(data[i]+"x"+constant+" should be "+expected[i], expected[i], result.toFloatArray()[i],0);
		}
	}
	
	@Test
    public void testAddDoubleExceptsOnNan() {
	    exception.expect(IllegalArgumentException.class);
        profile.add(Double.NaN);
	}
	
	@Test
    public void testAddDoubleExceptsOnPositiveInfinity() {
        exception.expect(IllegalArgumentException.class);
        profile.add(Double.POSITIVE_INFINITY);
    }
	
	@Test
    public void testAddDoubleExceptsOnNegativeInfinity() {
        exception.expect(IllegalArgumentException.class);
        profile.add(Double.NEGATIVE_INFINITY);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#subtract(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 */
	@Test
	public void testSubtract() {

		float[] f    = { 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10 };
		IProfile p2 = new FloatProfile(f);
		float[] exp  = { 0, -5, -9, -8, -3, 9, 2, -7, -1, 10, 3, -4, -6 };
		IProfile p1 = new FloatProfile(data);
		IProfile p3 = p1.subtract(p2);
		
		float[] result = p3.toFloatArray();
		
		for( int i =0;i<exp.length; i++){
			assertEquals(exp[i], result[i], 0);
		}
		
	}
	
	@Test
    public void testSubtractProfileExceptsOnDifferentLength() {
        float[] f    = { 10, 10 };
        IProfile p2 = new FloatProfile(f);
        exception.expect(IllegalArgumentException.class);
        IProfile p3 = profile.subtract(p2);
    }
	
	@Test
    public void testSubtractDouble() {

        float[] f    = { 0, 10, 5, 100 };
        IProfile p = new FloatProfile(f);
        double sub = 1;
        float[] exp  = { -1, 9, 4, 99 };

        IProfile r = p.subtract(sub);
        
        float[] result = r.toFloatArray();
        
        for( int i =0;i<exp.length; i++){
            assertEquals(exp[i], result[i], 0);
        }
        
    }
	
	@Test
    public void testSubtractDoubleExceptsOnNan() {
        exception.expect(IllegalArgumentException.class);
        profile.subtract(Double.NaN);
    }
    
    @Test
    public void testSubtractDoubleExceptsOnPositiveInfinity() {
        exception.expect(IllegalArgumentException.class);
        profile.subtract(Double.POSITIVE_INFINITY);
    }
    
    @Test
    public void testSubtractDoubleExceptsOnNegativeInfinity() {
        exception.expect(IllegalArgumentException.class);
        profile.subtract(Double.NEGATIVE_INFINITY);
    }


	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#merge(java.util.List)}.
	 */
	@Test
	public void testMerge() {
		
		float[] a1 = { 1, 2, 3, 4, 5 };
		float[] a2 = { 6, 7, 8 };
		float[] a3 = { 9, 10 };
		
		IProfile p1 = new FloatProfile(a1);
		IProfile p2 = new FloatProfile(a2);
		IProfile p3 = new FloatProfile(a3);
		
		List<IProfile> l = new ArrayList<>();
		l.add(p1);
		l.add(p2);
		l.add(p3);
		
		IProfile m = IProfile.merge(l);
		
		int expLength = a1.length+a2.length+a3.length;
		assertEquals(expLength, m.size());
		
		for(int i=0; i<10; i++){
		    assertEquals(i+1, m.get(i), 0);
		}
		
	}
	
	@Test
    public void testMergeExceptsOnNullList() {
	    exception.expect(IllegalArgumentException.class);
	    IProfile.merge(null);
	}
		
	@Test
	public void interpolationShouldLinearExtend() throws Exception{
		float[] data       = { 10, 11, 12, 13, 14, 15 };
		float[] expected   = { 10, 10.5f, 11, 11.5f, 12, 12.5f, 13, 13.5f, 14, 14.5f, 15, 12.5f };
		
		IProfile tester = new FloatProfile(data);
		IProfile result = tester.interpolate(12);
		float[] output = result.toFloatArray();	
		
		for( int i =0;i<expected.length; i++){
			assertEquals(output[i]+" should be "+expected[i], expected[i], output[i],0);
		}
		
	}
	
	@Test
	public void interpolationShouldShrinkWhenGivenLowerLength() throws Exception{
		float[] data       = { 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21 };
		float[] expected   = { 10, 12, 14, 16, 18, 20 };
		
		IProfile tester = new FloatProfile(data);
		IProfile result =  tester.interpolate(6);
		
		float[] output = result.toFloatArray();	
		
		for( int i =0;i<expected.length; i++){
			assertEquals(output[i]+" should be "+expected[i], expected[i], output[i],0);
		}

	}
	
	@Test
	public void squareDiffsAreCalculatedCorrectly() throws ProfileException{

		float[] test       = { 9, 20, 13, 6, 4, 10, 5, 1, 2, 7, 19, 12, 3 };
		
		IProfile dataProfile = new FloatProfile(data);
		IProfile templateProfile = new FloatProfile(test);
				
		// 1+15+12+4+3+9+7+2+7+13+6+6+1 
		double expectedDiff = 820;
		double value = dataProfile.absoluteSquareDifference(templateProfile);		
		assertEquals(value+" should be "+expectedDiff, expectedDiff, value,0);
		
	}
	
	@Test
	public void testGetWindowWithinCentreOfProfile(){
	    float[] test       = { 9, 20, 13, 6, 4, 10, 5, 1, 2, 7, 19, 12, 3 };
	    IProfile p = new FloatProfile(test);
	    
	    IProfile r = p.getWindow(5, 2);
	    float[] exp = { 6, 4, 10, 5, 1 };
	    
	    assertThat(r.size(), is(exp.length));
	    
	    for(int i=0; i<exp.length; i++){
	        assertThat(r.get(i), is((double)exp[i]));
	    }
	    
	}
	
	@Test
    public void testGetWindowAtStartOfProfile(){
        float[] test       = { 9, 20, 13, 6, 4, 10, 5, 1, 2, 7, 19, 12, 3 };
        IProfile p = new FloatProfile(test);
        
        IProfile r = p.getWindow(1, 2);
        float[] exp = { 3, 9, 20, 13, 6 };
        
        assertThat(r.size(), is(exp.length));
        
        for(int i=0; i<exp.length; i++){
            assertThat(r.get(i), is((double)exp[i]));
        }
        
    }
	
	@Test
    public void testGetWindowAtEndOfProfile(){
        float[] test       = { 9, 20, 13, 6, 4, 10, 5, 1, 2, 7, 19, 12, 3 };
        IProfile p = new FloatProfile(test);
        
        IProfile r = p.getWindow(11, 2);
        float[] exp = { 7, 19, 12, 3, 9 };
        
        assertThat(r.size(), is(exp.length));
        
        for(int i=0; i<exp.length; i++){
            assertThat(r.get(i), is((double)exp[i]));
        }
        
    }
	
	@Test
    public void testEqualsWithSameObjectRef(){
        assertTrue(profile.equals(profile));
    }
	
	@Test
    public void testEqualsWithSameData(){
	    IProfile p = new FloatProfile(data);
        assertTrue(profile.equals(p));
    }
	
	@Test
    public void testEqualsFalseWithNull(){
        assertFalse(profile.equals(null));
    }
	
	@Test
    public void testEqualsFalseWithNonProfile(){
	    Object o = new Object();
        assertFalse(profile.equals(o));
    }
	
	@Test
    public void testEqualsFalseWithDifferentData(){
        float[] d = new float[data.length];
        for(int i=0; i<data.length; i++){
            d[i] = data[i]+1;
        }
        IProfile p = new FloatProfile(d);
        assertFalse(profile.equals(p));
    }
	
	@Test
    public void testEqualsFalseWithSameDataInDifferentProfileType(){
	    double[] d = new double[data.length];
	    for(int i=0; i<data.length; i++){
	        d[i] = data[i];
	    }
	    IProfile p = new DoubleProfile(d);
        assertFalse(profile.equals(p));
    }
	
}
