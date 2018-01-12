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
package components.generic;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.generic.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.generic.FloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;


public class ProfileTest {
	
	float[] data       = { 10, 5, 1, 2, 7, 19, 12, 3, 9, 20, 13, 6, 4 }; // template data for a profile
	
	public ProfileTest(){
		
	}
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#FloatProfile(float[])}.
	 */
	@Test
	public void testFloatProfileFloatArrayWithNullData() {
		float[] data = null;
		exception.expect(IllegalArgumentException.class);
		IProfile tester = new FloatProfile(data);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#FloatProfile(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 */
	@Test
	public void testFloatProfileIProfile() {
		float[] data   = {0, 1, 2, 3,  4,  5 };

		IProfile tester = new FloatProfile(data);
		float[] result = new FloatProfile(tester).toFloatArray();
		
		for( int i =0;i<data.length; i++){
			assertEquals(data[i], result[i],0);
		}
		
		
		// Check null
		IProfile nullP = null;
		exception.expect(IllegalArgumentException.class);
		tester = new FloatProfile(nullP);
		
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
		IProfile p1 = new FloatProfile(data);
		int i = p1.size();
		assertEquals(i, data.length);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#equals(java.lang.Object)}.
	 */
	@Test
	public void testEqualsObject() {
		IProfile p1 = new FloatProfile(data);
		IProfile p2 = new FloatProfile(data);
		assertTrue(p1.equals(p2));
		
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#get(int)}.
	 */
	@Test
	public void testGetInt() {
		IProfile p1 = new FloatProfile(data);
		double d = p1.get(4);
		assertEquals( 7d , d, 0);
		
		exception.expect(IndexOutOfBoundsException.class);
		p1.get(-1);

		exception.expect(IndexOutOfBoundsException.class);
		p1.get(14);		
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#get(double)}.
	 */
	@Test
	public void testGetDouble() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getMax()}.
	 */
	@Test
	public void testGetMax() {
		IProfile p1 = new FloatProfile(data);
		assertEquals( 20d , p1.getMax(), 0);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getIndexOfMax(com.bmskinner.nuclear_morphology.components.generic.BooleanProfile)}.
	 */
	@Test
	public void testGetIndexOfMaxBooleanProfile() {
		
		// Restrict to first half of array
		BooleanProfile b = new BooleanProfile(data.length, false);
		for(int i=0; i<data.length/2; i++){
			b.set(i, true);
		}
		
		int expected = 5;
		
		try {
			
			IProfile p1 = new FloatProfile(data);
			int index = p1.getIndexOfMax(b);
			assertEquals( expected , index);
			
			// Test null
			exception.expect(IllegalArgumentException.class);
			index = p1.getIndexOfMax(null);
			
			// Test all false
			b = new BooleanProfile(data.length, false);
			exception.expect(ProfileException.class);
			index = p1.getIndexOfMax(b);
		
		
		} catch(ProfileException e){
			System.out.println("Error getting index: "+e.getMessage());
			fail("Index fetch failed");
		}
		
		
		
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getIndexOfMax()}.
	 */
	@Test
	public void testGetIndexOfMax() {
		IProfile p1 = new FloatProfile(data);
		try {
			assertEquals( 9 , p1.getIndexOfMax());
		} catch(ProfileException e){
			System.out.println("Error getting index: "+e.getMessage());
			fail("Index fetch failed");
		}
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
	 */
	@Test
	public void testGetIndexOfMinBooleanProfile() {
		// Restrict to second half of array
		BooleanProfile b = new BooleanProfile(data.length, true);
		for(int i=0; i<data.length/2; i++){
			b.set(i, false);
		}

		int expected = 7; // index
		
		try {
			
			IProfile p1 = new FloatProfile(data);
			int index = p1.getIndexOfMin(b);
			assertEquals( expected , index);
			
			// Test null
			exception.expect(IllegalArgumentException.class);
			index = p1.getIndexOfMin(null);
			
			// Test all false
			b = new BooleanProfile(data.length, false);
			exception.expect(ProfileException.class);
			index = p1.getIndexOfMin(b);
		
		
		} catch(ProfileException e){
			System.out.println("Error getting index: "+e.getMessage());
			fail("Index fetch failed");
		}
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getIndexOfMin()}.
	 */
	@Test
	public void testGetIndexOfMin() {
		
		IProfile p1 = new FloatProfile(data);

		try {
			assertEquals( 2 , p1.getIndexOfMin());
		} catch(ProfileException e){
			System.out.println("Error getting index: "+e.getMessage());
			fail("Index fetch failed");
		}
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
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getPositions(int)}.
	 */
	@Test
	public void testGetPositions() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getRescaledIndex(int, int)}.
	 */
	@Test
	public void testGetRescaledIndex() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#absoluteSquareDifference(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 */
	@Test
	public void testAbsoluteSquareDifference() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#copy()}.
	 */
	@Test
	public void testCopy() {
		IProfile p = new FloatProfile(data);
		float[] result = p.copy().toFloatArray();
		
		for( int i =0;i<data.length; i++){
			assertEquals(data[i], result[i],0);
		}
		
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#offset(int)}.
	 */
	@Test
	public void testOffset() {
		
		float[] exp1       = { 5, 1, 2, 7, 19, 12, 3, 9, 20, 13, 6, 4, 10 };
		float[] exp5       = { 19, 12, 3, 9, 20, 13, 6, 4, 10, 5, 1, 2, 7 };
		float[] exp_1      = { 4, 10, 5, 1, 2, 7, 19, 12, 3, 9, 20, 13, 6 }; // negative 1
		
		try {
			
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
			
			
		} catch (ProfileException e) {
			System.out.println(e.getMessage());
			fail("Offset failed");
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
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#interpolate(int)}.
	 */
	@Test
	public void testInterpolate() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getSlidingWindowOffset(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 */
	@Test
	public void testGetSlidingWindowOffset() {
		
		/*
		 * Testing with equal array lengths
		 */
		
		float[] test       = { 9, 20, 13, 6, 4, 10, 5, 1, 2, 7, 19, 12, 3 };

		int expectedOffset = 8;
		
		IProfile dataProfile = new FloatProfile(data);
		IProfile templateProfile = new FloatProfile(test);

		
		int offset = 0;
		try {
			offset = dataProfile.getSlidingWindowOffset(templateProfile);
		} catch (ProfileException e) {
			System.out.println("Error offsetting profile: "+e.getMessage());
			fail("Offsetting failed");
		}

		assertEquals(expectedOffset, offset,0);
		
		/*
		 * Testing with shorter test array
		 */
		
		float[] smallTest  = { 9, 16, 5, 7, 1.5f, 13, 7 };
		IProfile pTest = new FloatProfile(smallTest);
		
		offset = 0;
		try {
			offset = dataProfile.getSlidingWindowOffset(pTest);
		} catch (ProfileException e) {
			System.out.println("Error offsetting profile: "+e.getMessage());
			fail("Offsetting failed");
		}

		assertEquals(expectedOffset, offset,0);
		
		/*
		 * Testing with longer test array
		 */
		float[] longTest  = { 9, 14, 20, 16, 13, 9, 6, 5, 4, 7, 10, 7, 5, 2, 1, 1.5f, 2, 4, 7, 13, 19, 15, 12, 7, 3 };
		pTest = new FloatProfile(longTest);
		offset = 0;
		try {
			offset = dataProfile.getSlidingWindowOffset(pTest);
		} catch (ProfileException e) {
			System.out.println("Error offsetting profile: "+e.getMessage());
			fail("Offsetting failed");
		}
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
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getLocalMinima(int, double)}.
	 */
	@Test
	public void testGetLocalMinimaIntDouble() {
		fail("Not yet implemented");
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
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getLocalMaxima(int, double)}.
	 */
	@Test
	public void testGetLocalMaximaIntDouble() {
		fail("Not yet implemented");
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
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getSubregion(com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment)}.
	 */
	@Test
	public void testGetSubregionIBorderSegment() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#calculateDeltas(int)}.
	 */
	@Test
	public void testCalculateDeltas() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#differentiate()}.
	 */
	@Test
	public void testDifferentiate() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#log(double)}.
	 */
	@Test
	public void testLog() {
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
        float[] data       = {0, 1, 2, 3,  4,  5 };
        IProfile tester = new FloatProfile(data);
        
        tester = new FloatProfile(data);
        exception.expect(IllegalArgumentException.class);
        tester.divide(Double.NaN);
    }

	
	@Test
    public void testDivideDoublePositiveInfinityInputFails() {
        float[] data       = {0, 1, 2, 3,  4,  5 };
        IProfile tester = new FloatProfile(data);
        
        tester = new FloatProfile(data);
        exception.expect(IllegalArgumentException.class);
        tester.divide(Double.POSITIVE_INFINITY);
    }
	
	@Test
    public void testDivideDoubleNegativeInfinityInputFails() {
        float[] data       = {0, 1, 2, 3,  4,  5 };
        IProfile tester = new FloatProfile(data);
        
        tester = new FloatProfile(data);
        exception.expect(IllegalArgumentException.class);
        tester.divide(Double.NEGATIVE_INFINITY);
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
    public void testDivideIProfileFailsOnSizeMismatch() {
        float[] data  = {0, 1, 2,   3,  4,  5 };
        float[] div   = {1, 2, 0.5f, 3,  0.25f };

        IProfile tester = new FloatProfile(data);
        IProfile divider = new FloatProfile(div);

        exception.expect(IllegalArgumentException.class);
        tester.divide(divider);
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
		
		
		// Check odd inputs
		
		tester = new FloatProfile(data);
		exception.expect(IllegalArgumentException.class);
		tester.add(Double.NaN);
		
		exception.expect(IllegalArgumentException.class);
		tester.add(Double.POSITIVE_INFINITY);
		
		exception.expect(IllegalArgumentException.class);
		tester.add(Double.NEGATIVE_INFINITY);
		
				
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

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getRanks()}.
	 */
	@Test
	public void testGetRanks() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#getSortedIndexes()}.
	 */
	@Test
	public void testGetSortedIndexes() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.FloatProfile#merge(java.util.List)}.
	 */
	@Test
	public void testMerge() {
		fail("Not yet implemented");
	}
	
		
		
	@Test
	public void interpolationShouldLinearExtend(){
		float[] data       = { 10, 11, 12, 13, 14, 15 };
		float[] expected   = { 10, 10.5f, 11, 11.5f, 12, 12.5f, 13, 13.5f, 14, 14.5f, 15, 12.5f };
		
		IProfile tester = new FloatProfile(data);
		IProfile result = null;
		try {
			result = tester.interpolate(12);
		} catch (Exception e) {
			System.out.println("Error interpolating: "+e.getMessage());
			fail("Interpolation failed");
		}
		
		float[] output = result.toFloatArray();	
		
		for( int i =0;i<expected.length; i++){
			assertEquals(output[i]+" should be "+expected[i], expected[i], output[i],0);
		}
		
	}
	
	@Test
	public void interpolationShouldShrinkWhenGivenLowerLength(){
		float[] data       = { 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21 };
		float[] expected   = { 10, 12, 14, 16, 18, 20 };
		
		IProfile tester = new FloatProfile(data);
		
		IProfile result = null;
		try{
			result = tester.interpolate(6);
		} catch(ProfileException e){
			System.out.println("Error interpolating: "+e.getMessage());
			fail("Interpolation failed");
		}
		
		float[] output = result.toFloatArray();	
		
		for( int i =0;i<expected.length; i++){
//			System.out.println(output[i]+" should be "+expected[i]);
			assertEquals(output[i]+" should be "+expected[i], expected[i], output[i],0);
		}

	}
	
	@Test
	public void sortedValuesShouldReturnCorrectOrder(){
		float[] data       = { 10, 5, 1, 2, 7, 19, 12, 3 };
		float[] expected   = {  2, 3, 7, 1, 4,  0,  6, 5 };
		
		IProfile tester = new FloatProfile(data);
		System.out.println(tester.toString());
		
		IProfile result = tester.getSortedIndexes();

		float[] output = result.toFloatArray();	
		
		for( int i =0;i<expected.length; i++){
//			System.out.println(output[i]+" should be "+expected[i]);
			assertEquals(output[i]+" should be "+expected[i], expected[i], output[i],0);
		}
		
	}
	
	
	@Test
	public void squareDiffsAreCalculatedCorrectly(){

		float[] test       = { 9, 20, 13, 6, 4, 10, 5, 1, 2, 7, 19, 12, 3 };
		
		IProfile dataProfile = new FloatProfile(data);
		IProfile templateProfile = new FloatProfile(test);
				
		// 1+15+12+4+3+9+7+2+7+13+6+6+1 
		double expectedDiff = 820;
		double value = Double.NaN;
		try {
			value = dataProfile.absoluteSquareDifference(templateProfile);
		} catch (ProfileException e) {
			System.out.println("Error calculating difference: "+e.getMessage());
			fail("Difference failed");
		}
		
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


}
