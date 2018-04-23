//package com.bmskinner.nuclear_morphology.components.generic;
//
//import static org.junit.Assert.*;
//
//import java.util.List;
//import java.util.UUID;
//
//import org.junit.Before;
//import org.junit.Test;
//
//import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
//import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
//
//public class DefaultSegmentedProfileTest extends DefaultProfileTest{
//	
//	private ISegmentedProfile sProfile;
//	
//	@Override
//	@Before
//	public void setUp() throws UnavailableProfileTypeException{
//		super.setUp();
//		sProfile = comp.getProfile(ProfileType.ANGLE);
//	}
//
//	@Override
//	@Test
//	public void testEqualsObject() {
//		fail("Not yet implemented");
//	}
//
//	@Override
//	@Test
//	public void testReverse() {
//		super.testReverse();
//		List<IBorderSegment> segs = sProfile.getSegments();
//		int[] old = new int[segs.size()];
//		for(int i=0; i<old.length; i++){
//			old[i] = segs.get(i).getStartIndex();
//		}
//		
//		int[] exp = new int[old.length];
//		for(int i=0; i<old.length; i++){
//			exp[i] = sProfile.size()-1-old[old.length-i-1];
//		}
//
//		sProfile.reverse();
//		segs = sProfile.getSegments();
//		for(int i=0; i<old.length; i++){
//			assertEquals(exp[i], segs.get(i).getStartIndex());
//		}
//	}
//
//	@Test
//	public void testDefaultSegmentedProfileIProfileListOfIBorderSegment() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testDefaultSegmentedProfileISegmentedProfile() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testDefaultSegmentedProfileIProfile() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testDefaultSegmentedProfileFloatArray() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testHasSegments() {
//		assertTrue(sProfile.hasSegments());
//	}
//
//	@Test
//	public void testGetSegments() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetSegmentUUID() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testHasSegment() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetSegmentsFrom() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetOrderedSegments() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetSegmentString() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetSegmentIBorderSegment() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetSegmentAt() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetSegmentContaining() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetSegments() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testClearSegments() {
//		assertTrue(sProfile.hasSegments());
//		sProfile.clearSegments();
//		assertFalse(sProfile.hasSegments());
//	}
//
//	@Test
//	public void testGetSegmentNames() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetSegmentIDs() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetSegmentCount() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetDisplacement() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testContains() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testUpdate() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testAdjustSegmentStart() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testAdjustSegmentEnd() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testNudgeSegments() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testOffsetInt() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testInterpolateInt() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testFrankenNormaliseToProfile() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testMergeSegments() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testUnmergeSegment() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testIsSplittable() {
//		assertTrue(sProfile.isSplittable(sProfile.getSegmentContaining(1).getID(), 50));
//	}
//	
//	@Test
//	public void testIsSplittableReturnsFalseWhenTooSmall() {
//		assertFalse(sProfile.isSplittable(sProfile.getSegmentContaining(1).getID(), 2));
//	}
//
//	@Test
//	public void testSplitSegment() throws ProfileException {
//		sProfile.splitSegment(sProfile.getSegmentContaining(1), 50, UUID.randomUUID(), UUID.randomUUID());
//		assertEquals(2, sProfile.getSegmentCount());
//	}
//	
//	@Test
//	public void testSplitSegmentFailsWhenSegmentTooShort() throws ProfileException {
//		exception.expect(IllegalArgumentException.class);
//		sProfile.splitSegment(sProfile.getSegmentContaining(1), 2, UUID.randomUUID(), UUID.randomUUID());
//	}
//
//	@Test
//	public void testCopy() {
//		ISegmentedProfile p = sProfile.copy();
//		assertEquals(sProfile, p);
//	}
//}
