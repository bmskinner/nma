/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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
import java.util.List;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment;
import com.bmskinner.nuclear_morphology.components.generic.FloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;

public class SegmentedFloatProfileTest {
	
//	static List<IBorderSegment> list = new ArrayList<IBorderSegment>();
//	
//	static {
//		
//		DefaultBorderSegment p1 = new DefaultBorderSegment(10, 20, 100);
//		DefaultBorderSegment p2 = new DefaultBorderSegment(20, 45, 100);
//		DefaultBorderSegment p3 = new DefaultBorderSegment(45, 89, 100);
//		DefaultBorderSegment p4 = new DefaultBorderSegment(89, 10, 100);
//
//		list.add(p1);
//		list.add(p2);
//		list.add(p3);
//		list.add(p4);
//
//		try {
//			IBorderSegment.linkSegments(list);
//		} catch (ProfileException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	/**
	 * Create test segments in a linked list
	 * Segments run 10-20-45-89-10
	 * @return
	 */
	private List<IBorderSegment> makeTestSegments(){
		List<IBorderSegment> list = new ArrayList<IBorderSegment>();
		
		
		DefaultBorderSegment p1 = new DefaultBorderSegment(10, 20, 100, UUID.fromString("00000000-0000-0000-0000-000000000000"));
		DefaultBorderSegment p2 = new DefaultBorderSegment(20, 45, 100, UUID.fromString("11111111-1111-1111-1111-111111111111"));
		DefaultBorderSegment p3 = new DefaultBorderSegment(45, 89, 100, UUID.fromString("22222222-2222-2222-2222-222222222222"));
		DefaultBorderSegment p4 = new DefaultBorderSegment(89, 10, 100, UUID.fromString("33333333-3333-3333-3333-333333333333"));

		list.add(p1);
		list.add(p2);
		list.add(p3);
		list.add(p4);
		try {
			IBorderSegment.linkSegments(list);
		} catch (ProfileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}
	
	/**
	 * Creata a test profile with segments as in {@link #makeTestSegments()}
	 * @return
	 */
	private ISegmentedProfile makeTestProfile(){
		
		List<IBorderSegment> list = makeTestSegments();
		IProfile profile = new FloatProfile(10, 100);
		try {
			return new SegmentedFloatProfile(profile, list);
		} catch (ProfileException e) {

			e.printStackTrace();
			return null;
		}
	}
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Test
	public void testReverse() {
		fail("Not yet implemented");
	}

	@Test
	public void testSegmentedFloatProfileIProfileListOfIBorderSegment() {
		fail("Not yet implemented");
	}

	@Test
	public void testSegmentedFloatProfileISegmentedProfile() {
		fail("Not yet implemented");
	}

	@Test
	public void testSegmentedFloatProfileIProfile() {
		fail("Not yet implemented");
	}

	@Test
	public void testSegmentedFloatProfileFloatArray() {
		fail("Not yet implemented");
	}

	@Test
	public void testHasSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testSegmentIterator() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentUUID() {
		fail("Not yet implemented");
	}

	@Test
	public void testHasSegment() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentsFrom() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetOrderedSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentString() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentIBorderSegment() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentAt() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentContaining() {
		ISegmentedProfile sp = makeTestProfile();
//		10-20-45-89-10
		IBorderSegment s = sp.getSegmentContaining(0);
		assertEquals(UUID.fromString("33333333-3333-3333-3333-333333333333"), s.getID());
		
		try {
			
			sp = sp.offset(25);
			// Should now be 85-95-20-64-85
			 s = sp.getSegmentContaining(0);
			assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), s.getID());
			
			
		} catch (ProfileException e) {
			fail("Error in test");
			e.printStackTrace();
		}
		
		testGetSegmentContainingStartOutOfBounds();
		testGetSegmentContainingEndOutOfBounds();
	}
	
	@Test
	public void testGetSegmentContainingStartOutOfBounds(){
		ISegmentedProfile sp = makeTestProfile();
		exception.expect(IllegalArgumentException.class);
		sp.getSegmentContaining(-5);
	}
	
	@Test
	public void testGetSegmentContainingEndOutOfBounds(){
		ISegmentedProfile sp = makeTestProfile();
		exception.expect(IllegalArgumentException.class);
		sp.getSegmentContaining(100);
	}

	@Test
	public void testSetSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testClearSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentNames() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentIDs() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentCount() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetDisplacement() {
		fail("Not yet implemented");
	}

	@Test
	public void testContains() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdate() {
		fail("Not yet implemented");
	}

	@Test
	public void testAdjustSegmentStart() {
		fail("Not yet implemented");
	}

	@Test
	public void testAdjustSegmentEnd() {
		fail("Not yet implemented");
	}

	@Test
	public void testNudgeSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testOffsetInt() {

		try{
			/*
			 * Complete profile of segments
			 */
			List<IBorderSegment> list = makeTestSegments();
			IBorderSegment p1 = list.get(0);
			IBorderSegment p2 = list.get(1);
			IBorderSegment p3 = list.get(2);
			IBorderSegment p4 = list.get(3);


			p1.update(5, 20);
			assertEquals(5, p1.getStartIndex());
			assertEquals(5, p4.getEndIndex());

			p4.update(89, 1);
			assertEquals(1, p1.getStartIndex());
			assertEquals(1, p4.getEndIndex());

			/*
			 * Can the profile be offset and still have segments adjusted?
			 */
			ISegmentedProfile sp = makeTestProfile();

			sp = sp.offset(1);
			// Should now be 9-19-44-88-9
			assertEquals(9, sp.getSegment(p1.getID()).getStartIndex());
			assertEquals(9, sp.getSegment(p4.getID()).getEndIndex());
			
			sp = sp.offset(80);
			// Should now be 29-39-64-8-29
			assertEquals(29, sp.getSegment(p1.getID()).getStartIndex());
			assertEquals(39, sp.getSegment(p2.getID()).getStartIndex());
			assertEquals(64, sp.getSegment(p3.getID()).getStartIndex());
			assertEquals(8, sp.getSegment(p4.getID()).getStartIndex());
			
			

		} catch(SegmentUpdateException | ProfileException | UnavailableComponentException e){
			fail("Error updating segments");
			e.printStackTrace();
		}
	}

	@Test
	public void testInterpolateSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testFrankenNormaliseToProfile() {
		fail("Not yet implemented");
	}

	@Test
	public void testMergeSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testUnmergeSegment() {
		fail("Not yet implemented");
	}

	@Test
	public void testSplitSegment() {
		fail("Not yet implemented");
	}

	@Test
	public void testValueString() {
		fail("Not yet implemented");
	}

	@Test
	public void testFloatProfileFloatArray() {
		fail("Not yet implemented");
	}

	@Test
	public void testFloatProfileIProfile() {
		fail("Not yet implemented");
	}

	@Test
	public void testFloatProfileFloatInt() {
		fail("Not yet implemented");
	}

	@Test
	public void testCopy() {
		fail("Not yet implemented");
	}

	@Test
	public void testOffsetInt1() {
		fail("Not yet implemented");
	}

	@Test
	public void testInterpolate() {
		fail("Not yet implemented");
	}

}
