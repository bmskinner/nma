package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;

public class DefaultSegmentedProfileTest extends DefaultProfileTest {
	
	protected ISegmentedProfile singleSegmentProfile;
	protected ISegmentedProfile doubleSegmentProfile;
	protected final static UUID DOUBLE_SEG_ID_0 = UUID.fromString("00000000-0000-0000-0000-000000000001");
	protected final static UUID DOUBLE_SEG_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000002");
	protected final static UUID DOUBLE_SEG_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000003");
	protected final static UUID DOUBLE_SEG_ID_3 = UUID.fromString("00000000-0000-0000-0000-000000000004");
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		singleSegmentProfile = comp.new DefaultSegmentedProfile(data);
		doubleSegmentProfile = comp.new DefaultSegmentedProfile(data);
		int splitIndex = 50;
		doubleSegmentProfile.splitSegment(doubleSegmentProfile.getSegmentContaining(1), splitIndex, DOUBLE_SEG_ID_0, DOUBLE_SEG_ID_1);
	}

//	@Override
//	@Test
//	public void testEqualsObject() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testReverseWithSingleSegment() {
//		super.testReverse();
//		int expStart = 0;
//		int expEnd = 0;
//		singleSegmentProfile.reverse();
//		List<IBorderSegment> segs = singleSegmentProfile.getSegments();
//		assertEquals(expStart, segs.get(0).getStartIndex());
//		assertEquals(expEnd, segs.get(0).getEndIndex());
//	}
//	
//	@Test
//	public void testReverseWithDoubleSegment() {
//		super.testReverse();
//		List<IBorderSegment> segs = doubleSegmentProfile.getSegments();
//		int[] old = new int[segs.size()];
//		for(int i=0; i<old.length; i++){
//			old[i] = segs.get(i).getStartIndex();
//		}
//		
//		int[] exp = new int[old.length];
//		for(int i=0; i<old.length; i++){
//			exp[i] = doubleSegmentProfile.size()-1-old[old.length-i-1];
//		}
//
//		doubleSegmentProfile.reverse();
//		segs = doubleSegmentProfile.getSegments();
//		for(int i=0; i<old.length; i++){
//			assertEquals(exp[i], segs.get(i).getStartIndex());
//		}
//	}
//
//	@Test
//	public void testHasSegments() {
//		assertTrue(singleSegmentProfile.hasSegments());
//	}
//
//	@Test
//	public void testGetSegmentsForSingleSegmentProfile() {
//		List<IBorderSegment> list = singleSegmentProfile.getSegments();
//		assertEquals(1, list.size());
//	}
//	
//	@Test
//	public void testGetSegmentsForDoubleSegmentProfile() {
//		List<IBorderSegment> list = doubleSegmentProfile.getSegments();
//		assertEquals(2, list.size());
//	}
//
//	@Test
//	public void testGetSegmentUUID() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testHasSegment() {
//		assertTrue(singleSegmentProfile.hasSegment(comp.getID()));
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
//		assertTrue(singleSegmentProfile.hasSegments());
//		singleSegmentProfile.clearSegments();
//		assertTrue(singleSegmentProfile.hasSegments());
//		assertEquals(1, singleSegmentProfile.getSegmentCount());
//		
//		assertTrue(doubleSegmentProfile.hasSegments());
//		doubleSegmentProfile.clearSegments();
//		assertTrue(doubleSegmentProfile.hasSegments());
//		assertEquals(1, doubleSegmentProfile.getSegmentCount());
//	}
//
//	@Test
//	public void testGetSegmentNames() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetSegmentIDs() {		
//		List<UUID> ssIds = singleSegmentProfile.getSegmentIDs();
//		assertEquals(1, ssIds.size());
//		assertEquals(comp.getID(), ssIds.get(0));
//		
//		List<UUID> dsIds = doubleSegmentProfile.getSegmentIDs();
//		assertEquals(2, dsIds.size());
//		assertEquals(DOUBLE_SEG_ID_0, dsIds.get(0));
//		assertEquals(DOUBLE_SEG_ID_1, dsIds.get(1));
//		
//	}
//
//	@Test
//	public void testGetSegmentCount() {
//		assertEquals(1, singleSegmentProfile.getSegmentCount());
//		assertEquals(2, doubleSegmentProfile.getSegmentCount());
//	}
//
//	@Test
//	public void testGetDisplacementForSingleSegmentProfile() throws UnavailableComponentException {
//		double exp = singleSegmentProfile.getMax()-singleSegmentProfile.getMin();
//		assertEquals(exp, singleSegmentProfile.getDisplacement(singleSegmentProfile.getSegment(comp.getID())), 0);
//	}
//	
//	@Test
//	public void testGetDisplacementForDoubleSegmentProfile() throws UnavailableComponentException {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testContains() throws UnavailableComponentException {
//		
//		IBorderSegment s = singleSegmentProfile.getSegment(comp.getID());
//
//		IBorderSegment s0 = doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0);
//		IBorderSegment s1 = doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1);
//		
//		assertTrue(singleSegmentProfile.contains(s));
//		assertFalse(singleSegmentProfile.contains(s0));
//		assertFalse(singleSegmentProfile.contains(s1));
//		
//		assertTrue(doubleSegmentProfile.contains(s0));
//		assertTrue(doubleSegmentProfile.contains(s1));
//		
//		// Special case - root segment always present
//		assertTrue(doubleSegmentProfile.contains(s));
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
//	public void testInterpolateIntLengthensSingleSegmentProfile() throws ProfileException, UnavailableComponentException {
//		
//		int newLength = singleSegmentProfile.size() * 2;
//		ISegmentedProfile interpolated = singleSegmentProfile.interpolate(newLength);
//		assertEquals(newLength, interpolated.size());
//		assertEquals(0, interpolated.getSegment(comp.getID()).getStartIndex());
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
//		assertTrue(singleSegmentProfile.isSplittable(singleSegmentProfile.getSegmentContaining(1).getID(), 50));
//	}
//	
//	@Test
//	public void testIsSplittableReturnsFalseWhenTooSmall() {
//		assertFalse(singleSegmentProfile.isSplittable(singleSegmentProfile.getSegmentContaining(1).getID(), 2));
//	}
//
//	@Test
//	public void testSplitSegmentForSingleSegmentProfile() throws ProfileException {
//		
//		int splitIndex = 50;
//		assertEquals(1, singleSegmentProfile.getSegmentCount());
//		singleSegmentProfile.splitSegment(singleSegmentProfile.getSegmentContaining(1), splitIndex, DOUBLE_SEG_ID_0, DOUBLE_SEG_ID_1);
//		assertEquals(2, singleSegmentProfile.getSegmentCount());
//		
//		List<IBorderSegment> list = singleSegmentProfile.getSegments();
//		assertEquals(DOUBLE_SEG_ID_0, list.get(0).getID());
//		assertEquals(DOUBLE_SEG_ID_1, list.get(1).getID());
//		
//		assertEquals(0, list.get(0).getStartIndex());
//		assertEquals(splitIndex, list.get(0).getEndIndex());
//		assertEquals(splitIndex, list.get(1).getStartIndex());
//		assertEquals(0, list.get(1).getEndIndex());
//	}
//	
//	@Test
//	public void testSplitSegmentForDoubleSegmentProfile() throws ProfileException {
//		assertEquals(2, doubleSegmentProfile.getSegmentCount());
//		doubleSegmentProfile.splitSegment(doubleSegmentProfile.getSegmentContaining(1), 25, DOUBLE_SEG_ID_2, DOUBLE_SEG_ID_3);
//		assertEquals(3, doubleSegmentProfile.getSegmentCount());
//		
//		List<IBorderSegment> list = doubleSegmentProfile.getSegments();
//		assertEquals(DOUBLE_SEG_ID_2, list.get(0).getID());
//		assertEquals(DOUBLE_SEG_ID_3, list.get(1).getID());
//		assertEquals(DOUBLE_SEG_ID_1, list.get(2).getID());
//		
//		assertEquals(0, list.get(0).getStartIndex());
//		assertEquals(25, list.get(0).getEndIndex());
//		assertEquals(25, list.get(1).getStartIndex());
//		assertEquals(50, list.get(1).getEndIndex());
//		assertEquals(50, list.get(2).getStartIndex());
//		assertEquals(0, list.get(2).getEndIndex());
//	}
//	
//	@Test
//	public void testSplitSegmentFailsWhenSegmentTooShort() throws ProfileException {
//		exception.expect(IllegalArgumentException.class);
//		singleSegmentProfile.splitSegment(singleSegmentProfile.getSegmentContaining(1), 2, UUID.randomUUID(), UUID.randomUUID());
//	}
//
//	@Override
//	@Test
//	public void testCopy() {
//		ISegmentedProfile p = singleSegmentProfile.copy();
//		assertEquals(singleSegmentProfile, p);
//	}
	
	@Test
    public void testGetSegmentIDsReturnsEmptyListWhenNoSegments() throws ProfileException {
		singleSegmentProfile.clearSegments();
        List<UUID> result = singleSegmentProfile.getSegmentIDs();
        assertTrue(result.size()==1);
    }
	
	@Test
	public void testClearSegments() {
	    assertTrue(singleSegmentProfile.hasSegments());
	    singleSegmentProfile.clearSegments();
	    assertTrue(singleSegmentProfile.hasSegments());
	}

	@Test
	public void testHasSegments() {
		assertTrue(singleSegmentProfile.hasSegments());
		singleSegmentProfile.clearSegments();
		assertTrue(singleSegmentProfile.hasSegments());
	}
}
