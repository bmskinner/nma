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
package test.no.components;

import static org.junit.Assert.*;

import java.util.List;

import no.components.NucleusBorderSegment;

import org.junit.Test;

public class NucleuBordersSegmentTest {
	
	@Test
	public void testUpdatingASegmentWithInvalidPositions(){
		
		NucleusBorderSegment testSegment = new NucleusBorderSegment(0, 20, 100);
		try{
			testSegment.update(-1, 21);
			fail("Segment update should not allow out of range value");
		} catch (IllegalArgumentException e){
			assertEquals("Error should be fail", e.getMessage(), "Start index is outside the profile range: -1");
		}
		
		try{
			testSegment.update(0, 101);
			fail("Segment update should not allow out of range value");
		} catch (IllegalArgumentException e){
			assertEquals("Error should be fail", e.getMessage(), "End index is outside the profile range: 101");
		}
	}
	
	@Test
	public void testUpdatingASegmentTooShort(){
		
		
		// A single segment 
		NucleusBorderSegment testSegment = new NucleusBorderSegment(0, 20, 100);
		NucleusBorderSegment prevSegment = new NucleusBorderSegment(90, 0, 100);
		NucleusBorderSegment nextSegment = new NucleusBorderSegment(20, 30, 100);
		
		
		try{
			testSegment.update(0, 3);
			fail("Segment update should not allow too short length");
		} catch (IllegalArgumentException e){
			assertEquals("Error should be fail", e.getMessage(), "Segment length cannot be smaller than "+NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH);
		}
		
		try{
			testSegment.update(17, 20);
			fail("Segment update should not allow too short length");
		} catch (IllegalArgumentException e){
			assertEquals("Error should be fail", e.getMessage(), "Segment length cannot be smaller than "+NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH);
		}
		
		// Segment chain
		testSegment = new NucleusBorderSegment(0, 20, 100);
		testSegment.setPrevSegment(prevSegment);
		testSegment.setNextSegment(nextSegment);
		prevSegment.setNextSegment(testSegment);
		nextSegment.setPrevSegment(testSegment);
					
		// check the previous segment
		assertFalse(testSegment.update(	94, 20));
		assertTrue(testSegment.update(	95, 20));

		// check the next segment
		assertTrue(testSegment.update(	0, 25));
		assertFalse(testSegment.update(	0, 26));


		
	}
	
	
	@Test
	public void testUpdatingASegmentToInvert(){
		
		NucleusBorderSegment testSegment = new NucleusBorderSegment(1, 20, 100);
		try{
			testSegment.update(26, 20);
			fail("Segment update should not allow inversion");
		} catch (IllegalArgumentException e){
			assertEquals("Error should be fail", e.getMessage(), "Operation would cause this segment to invert");
		}
	}
	
//	@Test
//	public void testUpdatingASegemntWithInvalidPositons() {
//				
//		List<NucleusBorderSegment> segments = SegmentFitterTest.getMedianRodentSpermSegments();
//		
//		NucleusBorderSegment testSegment = segments.get(0);
//		
//		
//		
//		
//		fail("Not yet implemented");
//	}

}
