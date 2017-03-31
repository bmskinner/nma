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
package components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.generic.FloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;


public class ProfileTest {
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Test
	public void profileShouldNotBeCreatedWithNullData(){
		float[] data = null;
		exception.expect(IllegalArgumentException.class);
		IProfile tester = new FloatProfile(data);
	}
	
	@Test
	public void profileCanBeCreatedFromProfile(){
		float[] data   = {0, 1, 2, 3,  4,  5 };

		IProfile tester = new FloatProfile(data);
		IProfile result = new FloatProfile(tester);
		
		for( int i =0;i<data.length; i++){
			assertEquals("Values should be identical", data[i], result.toFloatArray()[i],0);
		}
	}
	
	
	@Test
	public void profileShouldErrorOnOutOfLowerBoundsRequest(){
		float[] data   = {0, 1, 2, 3,  4,  5 };
		IProfile tester = new FloatProfile(data);
		exception.expect(IndexOutOfBoundsException.class);
		tester.get(-1);
	}
	
	
	@Test
	public void profileShouldErrorOnOutOfUpperBoundsRequest(){
		float[] data   = {0, 1, 2, 3,  4,  5 };
		IProfile tester = new FloatProfile(data);
		exception.expect(IndexOutOfBoundsException.class);
		tester.get(6);
	}
	
	@Test
	public void profileShouldGetInBoundsRequest(){
		
		float[] data   = {0, 1, 2, 3,  4,  5 };
		IProfile tester = new FloatProfile(data);
		
		double d = tester.get(0);
		assertEquals("Value should be 0", 0, d,0);

		d = tester.get(1);
		assertEquals("Value should be 1", 1, d,0);
		
		d = tester.get(2);
		assertEquals("Value should be 2", 2, d,0);
				
	}
	
	@Test
	public void profileShouldGetMinAndMax(){
		
		float[] data   = {-1, 0, 2, 3,  4,  5 };
		IProfile tester = new FloatProfile(data);
				
		assertEquals("Min should should be -1", -1, tester.getMin(),0);
		assertEquals("Max should should be 5" , 5 , tester.getMax(),0);
		
		assertEquals("Min index should be 0", 0, tester.getIndexOfMin(),0);
		assertEquals("Max index should be 5", 5, tester.getIndexOfMax(),0);
			
		for( int i =0;i<data.length; i++){
			assertEquals("Data within array should be identical to input", data[i], tester.toFloatArray()[i],0);
		}
	}
	
	
	@Test
	public void profileSizeShouldBeArrayLength(){
		float[] data     = {1, 1, 1, 1, 1, 1};
		
		IProfile tester = new FloatProfile(data);
		
		assertEquals("Profile length should be 6", data.length, tester.size(),0);
	}

	@Test
	public void multiplicationByConstantShouldReturnConstant() {

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
	public void multiplicationByProfileShouldReturnVariable() {

		// MyClass is tested
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
	public void additionByConstantShouldReturnConstant() {

		// MyClass is tested
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
		} catch(Exception e){
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
	public void bestFittingReturnsCorrectOffset(){
		float[] data       = { 10, 5, 1, 2, 7, 19, 12, 3, 9, 20, 13, 6, 4 };
		float[] test       = { 9, 20, 13, 6, 4, 10, 5, 1, 2, 7, 19, 12, 3 };

		int expectedOffset = 5;
		
		IProfile dataProfile = new FloatProfile(data);
		IProfile templateProfile = new FloatProfile(test);

		
		int offset = 0;
		try {
			offset = dataProfile.getSlidingWindowOffset(templateProfile);
		} catch (ProfileException e) {
			System.out.println("Error offsetting profile: "+e.getMessage());
			fail("Offsetting failed");
		}

		assertEquals(offset+" should be "+expectedOffset, expectedOffset, offset,0);
		
	}
	
	@Test
	public void squareDiffsAreCalculatedCorrectly(){
		float[] data       = { 10, 5, 1, 2, 7, 19, 12, 3, 9, 20, 13, 6, 4 };
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


}
