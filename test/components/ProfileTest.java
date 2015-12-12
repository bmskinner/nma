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

import org.junit.Test;

import components.generic.Profile;
import components.generic.SegmentedProfile;


public class ProfileTest {
	
	@Test
	public void profileShouldNotBeCreatedWithNullData(){
		double[] data = null;

		try {
			Profile tester = new Profile(data);
			fail("Profile should not be created with null input");
		} catch (Exception e) {
			// expected
			// could also check for message of exception, etc.
		} 

	}
	
	@Test
	public void profileCanBeCreatedFromProfile(){
		double[] data   = {0, 1, 2, 3,  4,  5 };

		Profile tester = new Profile(data);
		Profile result = new Profile(tester);
		
		for( int i =0;i<data.length; i++){
			assertEquals("Values should be identical", data[i], result.asArray()[i],0);
		}
	}
	
	
	@Test
	public void profileShouldErrorOnOutOfBoundsRequest(){
		
		double[] data   = {0, 1, 2, 3,  4,  5 };
		Profile tester = new Profile(data);
		
		double d = tester.get(2);
		assertEquals("Values should be 2", 2, d,0);
		
		assertEquals("Out of range values should be 0", 0, tester.get(-1),0);
		assertEquals("Out of range values should be 0", 0, tester.get(6),0);
				
	}
	
	@Test
	public void profileShouldGetMinAndMax(){
		
		double[] data   = {-1, 0, 2, 3,  4,  5 };
		Profile tester = new Profile(data);
				
		assertEquals("Min should hould be -1", -1, tester.getMin(),0);
		assertEquals("Max should hould be 5" , 5 , tester.getMax(),0);
		
		assertEquals("Min index should hould be 0", 0, tester.getIndexOfMin(),0);
		assertEquals("Max index should hould be 5", 5, tester.getIndexOfMax(),0);
			
		for( int i =0;i<data.length; i++){
			assertEquals("Data within array should be identical to input", data[i], tester.asArray()[i],0);
		}
	}
	
	
	@Test
	public void profileSizeShouldBeArrayLength(){
		double[] data     = {1, 1, 1, 1, 1, 1};
		
		Profile tester = new Profile(data);
		
		assertEquals("Profile length should be 6", data.length, tester.size(),0);
	}

	@Test
	public void multiplicationByConstantShouldReturnConstant() {

		// MyClass is tested
		double[] data     = {1, 1, 1, 1, 1, 1};
		double   constant = 2;
		double[] expected = {2 ,2, 2, 2, 2, 2};
		
		Profile tester = new Profile(data);
		Profile result = tester.multiply(constant);

		// assert statements
		
		// there is no assertArrayEquals for double[]
		for( int i =0;i<data.length; i++){
			assertEquals("1x2 should be 2", expected[i], result.asArray()[i],0);
		}

	}
	
	@Test
	public void multiplicationByProfileShouldReturnVariable() {

		// MyClass is tested
		double[] data       = {0, 1, 2, 3,  4,  5 };
		double[] multiplier = {1, 2, 3, 4,  5,  6 };
		double[] expected   = {0, 2, 6, 12, 20, 30};
		
		Profile tester = new Profile(data);
		Profile multiply = new Profile(multiplier);
		Profile result = tester.multiply(multiply);
		
		// there is no assertArrayEquals for double[]
		for( int i =0;i<data.length; i++){
			assertEquals(data[i]+"x"+multiplier[i]+"should be "+expected[i], expected[i], result.asArray()[i],0);
		}
	}
	
	@Test
	public void additionByConstantShouldReturnConstant() {

		// MyClass is tested
		double[] data       = {0, 1, 2, 3,  4,  5 };
		double   constant   = 2;
		double[] expected   = {2, 3, 4, 5, 6, 7};
		
		Profile tester = new Profile(data);
		Profile result = tester.add(constant);
		
		// there is no assertArrayEquals for double[]
		for( int i =0;i<data.length; i++){
			assertEquals(data[i]+"x"+constant+" should be "+expected[i], expected[i], result.asArray()[i],0);
		}
	}
	
	@Test
	public void interpolationShouldLinearExtend(){
		double[] data       = { 10, 11, 12, 13, 14, 15 };
		double[] expected   = { 10, 10.5, 11, 11.5, 12, 12.5, 13, 13.5, 14, 14.5, 15, 12.5 };
		
		Profile tester = new Profile(data);
		Profile result = tester.interpolate(12);
		
		double[] output = result.asArray();	
		
		for( int i =0;i<expected.length; i++){
			assertEquals(output[i]+" should be "+expected[i], expected[i], output[i],0);
		}
		
	}
	
	@Test
	public void interpolationShouldShrinkWhenGivenLowerLength(){
		double[] data       = { 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21 };
		double[] expected   = { 10, 12, 14, 16, 18, 20 };
		
		Profile tester = new Profile(data);
		
		Profile result = null;
		try{
			result = tester.interpolate(6);
		} catch(Exception e){
			System.out.println("Error interpolating: "+e.getMessage());
			fail("Interpolation failed");
		}
		
		double[] output = result.asArray();	
		
		for( int i =0;i<expected.length; i++){
//			System.out.println(output[i]+" should be "+expected[i]);
			assertEquals(output[i]+" should be "+expected[i], expected[i], output[i],0);
		}
		
	}
	
	@Test
	public void sortedValuesShouldReturnCorrectOrder(){
		double[] data       = { 10, 5, 1, 2, 7, 19, 12, 3 };
		double[] expected   = {  2, 3, 7, 1, 4,  0,  6, 5 };
		
		Profile tester = new Profile(data);
		System.out.println(tester.toString());
		
		Profile result = null;
		try{
			result = tester.getSortedIndexes();
		} catch(Exception e){
			System.out.println("Error getting sorted values: "+e.getMessage());
			fail("Interpolation failed");
		}
		
		double[] output = result.asArray();	
		
		for( int i =0;i<expected.length; i++){
//			System.out.println(output[i]+" should be "+expected[i]);
			assertEquals(output[i]+" should be "+expected[i], expected[i], output[i],0);
		}
		
	}
	
	@Test
	public void rodentProfileShouldGiveCorrectVertical(){
		
//		SegmentedProfile p = IndividualNuclei.rodentSpermMedianProfile();
//		int[] i = p.getConsistentRegionBounds(180, 2, 5);
//		System.out.println("Start: "+i[0]);
//		System.out.println("End  : "+i[1]);
		
	}

}
